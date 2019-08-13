
// docs
// https://support.cloudbees.com/hc/en-us/articles/217708168-create-credentials-from-groovy
// https://gist.github.com/ivan-pinatti/830ec918781060df03b12efd4a14096e

// only Groovy 3.0 has native yaml support
import groovy.json.JsonSlurper
@Grab('org.yaml:snakeyaml:1.25')

import org.yaml.snakeyaml.Yaml

import jenkins.model.*
import com.cloudbees.hudson.plugins.folder.*;
import com.cloudbees.hudson.plugins.folder.properties.*;
import com.cloudbees.hudson.plugins.folder.properties.FolderCredentialsProvider.FolderCredentialsProperty;
import com.cloudbees.plugins.credentials.impl.*;
import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.domains.*;



Map itemCache = [:]
String opToken = null
Jenkins instance = Jenkins.instance

def signin(String domain, String username, String password, String masterSecret) {
    println "signing in to 1Password using credentials for: ${username} in ${domain}."


    // usage: op signin <signinaddress> <emailaddress> <secretkey> [--output=raw]


    String[] env_vars = []
    File op_pwd = (System.getProperty('user.home') as File)

    Process op_proc = 'echo $OP_PASSWORD'.execute(env_vars,op_pwd).pipeTo('op signin $OP_DOMAIN $OP_USERNAME $OP_MASTER_KEY --output=raw'.execute(env_vars, op_pwd))

    int exit_code = op_proc.exitValue()

    if (exit_code) {
        println "failed to signin:"
        println op_proc.errorStream.readLines().join("\n")
        return null
    }


    // signin was successful, return token
    println "1Password signin was success, I got a token."
    String onePassToken = op_proc.getText().trim()
    opToken = onePassToken

    return onePassToken
}

def String lookup(String itemName, String vaultName=null, String sectionName=null, String fieldName = 'password') {

    Map raw = raw(itemName, vaultName)
    String lookupValue = null

    if (sectionName) {
        for (section in raw.sections) {
            if (section.title == sectionName) {
                for (field in section.fields) {
                    if (field.t == fieldName) {
                        lookupValue = field.v
                    }
                }
            }
        }
    }
    // we look in default fields
    else {
        for (Map field in raw.details.fields) {
            if (field.name == fieldName) {
                lookupValue = field.value
                break
            }
        }
    }

    println "lookup value: ${lookupValue}"
    return lookupValue
}

// groovy method caching
//@Memoized(maxCacheSize=100)
def Map raw(String itemName, String vault = null) {

    def cached_item = itemCache.get(itemName)
    if (cached_item) {
        println "returning item '${itemName}' from cache."
        return cached_item
    }

    println "finding item ${itemName} in ${vault}"
    String vault_param = vault ? "--vault=${vault}" : ""

    // avoid exposing the token in job logs
    String[] env_vars = ["OP_TOKEN=${opToken}"]
    File op_pwd = (System.getProperty('user.home') as File)


    Process lookup_proc = 'echo $OP_TOKEN | ' + "op get item ${itemName} ${vault_param}".execute(env_vars, op_pwd)

    if (lookup_proc.exitValue()) {
        println "failed to execute lookup of item ${itemName}"
        return null
    }

    JsonSlurper slurper = new JsonSlurper()
    Map item_data = slurper.parse(lookup_proc.getText())

    //echo "raw item data: ${item_raw}"

    itemCache[itemName] = item_data
    return item_data
}


def Domain createCredentialsDomain(Map domainConfig=null) {
    if(domainConfig == null || domainConfig.isEmpty()) {
        def global = Domain.global()
        println "global domain: ${global}"
        return global
    }

    List<DomainSpecification> specs = [
            new HostnameSpecification(domainConfig.includes, domainConfig.excludes)
    ]

    return new Domain(domainConfig.name, domainConfig.description, specs)
}

def Object credentialStoreForFolder(String folderName) {
    Jenkins jenkins = Jenkins.getInstance()
    for (folder in jenkins.getAllItems(Folder.class)) {
        if(folder.name.equals(folderName)){
            AbstractFolder<?> folderAbs = AbstractFolder.class.cast(folder)
            FolderCredentialsProperty property = folderAbs.getProperties().get(FolderCredentialsProperty.class)

            // add FolderCredentialsProperty if it is not there yet
            if (property == null) {
                property = new FolderCredentialsProvider.FolderCredentialsProperty([])
                folderAbs.addProperty(property)
            }

            return property.getStore()

            //property.getStore().addCredentials(Domain.global(), c)
            // println property.getCredentials().toString()
        }
    }

    println "failed to locate credentials store for folder ${folderName}"
    return null
}


def processDomainCredentials(List<Map> domainCredentialConfigList, Object credentialStore) {

    for (Map domainCredentialConfig in domainCredentialConfigList) {

        Map domainData = domainCredentialConfig.get('domain', null)
        println "domainData: ${domainData}"
        Domain domain = createCredentialsDomain(domainData)
        println "domain: ${domain}"
        List<Map> credentialData = domainCredentialConfig.credentials

        List<Credentials> credList = []
        for (Map credential in credentialData) {
            println "processing credential ${credential.id}"
            Credentials cred = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credential.id, credential.description, "user", "password")
            //credentialStore.addCredentials(domain, cred)
            credList.add(cred)
        }

        credentialStore.addDomain(domain, credList)
    }
}


def removeAllDomains(store) {
    List<Domain> allDomains = store.getDomains()
    println "wiping domains: ${allDomains}"
    allDomains.each { domain ->
        store.removeDomain(domain)
    }
}

println "CREDENTIALS ==================================================================================================="
println "WARNING credentials bootstrap from init script is not implemented. doing nothing."
String configFilePath = "${System.getProperty('user.home')}/initial_credentials.yaml"

File credentialsFile = new File(configFilePath)
if(!credentialsFile.exists()) {
    println "no such file: ${configFilePath} will not try to do setup"
    return
}

Yaml parser = new Yaml()
Map credentialsConfig = parser.load((configFilePath as File).text)

println "i got this from yaml: ${credentialsConfig}"

def jenkinsCredentialsGlobalDomain = Domain.global()
println "top level global domain: ${jenkinsCredentialsGlobalDomain}"

List<Map> systemCredentialsConfig = credentialsConfig.get('system', [])
println "adding globally visible credentials"

def systemStore = SystemCredentialsProvider.getInstance().getStore()
removeAllDomains(systemStore)
instance.save()

processDomainCredentials(systemCredentialsConfig, systemStore)
instance.save()

// adding per folder credentials, if any
Map<String,Map> folderCredentialsConfig = credentialsConfig.get('folder', [:])
println "adding folder credentials"
folderCredentialsConfig.each {
    String folderName = it.key
    List<Map> folder_credentials = it.value
    println "adding credentials for folder '${folderName}'"

    def folderStore = credentialStoreForFolder(folderName)
    removeAllDomains(folderStore)
    processDomainCredentials(folder_credentials, folderStore)
}

// persist all changes
instance.save()
