// docs
// https://support.cloudbees.com/hc/en-us/articles/217708168-create-credentials-from-groovy
// https://gist.github.com/ivan-pinatti/830ec918781060df03b12efd4a14096e

// only Groovy 3.0 has native yaml support
import groovy.json.JsonSlurper
@Grab('org.yaml:snakeyaml:1.25')

import org.yaml.snakeyaml.Yaml

import jenkins.model.*;
import hudson.util.Secret;
import com.cloudbees.plugins.credentials.SecretBytes;
import com.cloudbees.hudson.plugins.folder.*;
import com.cloudbees.hudson.plugins.folder.properties.*;
import com.cloudbees.hudson.plugins.folder.properties.FolderCredentialsProvider.FolderCredentialsProperty;
import com.cloudbees.plugins.credentials.impl.*;
import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.domains.*;

import org.jenkinsci.plugins.plaincredentials.impl.*;
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl
import java.lang.StringBuffer;

Map itemCache = [:]
String opToken = null
Jenkins instance = Jenkins.instance

def signin(String domain, String username, String password, String masterSecret) {
    println "signing in to 1Password using credentials for: ${username} in ${domain}."

    // usage: op signin <signinaddress> <emailaddress> <secretkey> [--output=raw]
    Process op_proc = "op signin ${domain} ${username} ${masterSecret} --output=raw".execute()
    // send password to process
    def writer = op_proc.out.newPrintWriter()
    writer.println(password)
    writer.flush()

    //op_proc.waitForOrKill(10000)

    StringBuffer stdout = new StringBuffer()
    StringBuffer stderr = new StringBuffer()
    op_proc.waitForProcessOutput(stdout, stderr)

    int exit_code = op_proc.exitValue()

    if (exit_code) {
        println "failed to signin:"
        println "${stderr}"
        return null
    }

    // signin was successful, return token
    println "1Password signin was success, I got a token."
    String onePassToken = stdout.toString().trim()
    this.opToken = onePassToken

    return onePassToken
}

def String lookup(String itemName, String vaultName=null, String sectionName=null, String fieldName = 'password') {

    Map raw = raw(itemName, vaultName)
    if (raw == null) {
        return null
    }
    String lookupValue = null

    if (sectionName) {
        for (section in raw.details.sections) {
            if (section.title == sectionName) {
                for (field in section.fields) {
                    if (field.t == fieldName) {
                        lookupValue = field.v
                        break
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

    //println "lookup value '${sectionName}->${fieldName}': ${lookupValue}"
    return lookupValue
}

// groovy method caching
//@Memoized(maxCacheSize=100)
def Map raw(String itemName, String vault = null) {

    def cached_item = this.itemCache.get(itemName)
    if (cached_item) {
        println "returning item '${itemName}' from cache."
        return cached_item
    }

    println "finding item ${itemName} in ${vault}"
    String vault_param = vault ? "--vault=${vault}" : ""

    // avoid exposing the token in job logs
    //String[] env_vars = ["OP_TOKEN=${this.opToken}"]
    //File op_pwd = (System.getProperty('user.home') as File)


    ProcessBuilder builder = new ProcessBuilder()
    builder.command(["op", "get", "item", itemName, vault_param])
    builder.environment().put("OP_TOKEN", this.opToken)
    builder.directory((System.getProperty('user.home') as File))

    Process lookup_proc = builder.start()

    // write 1pass session token to stdin
    def writer = lookup_proc.out.newPrintWriter()
    writer.println(this.opToken)
    writer.flush()

    lookup_proc.waitForOrKill(10000)
    if (lookup_proc.exitValue()) {
        println "failed to execute lookup of item '${itemName}':"
        println "${lookup_proc.err}"
        return null
    }

    JsonSlurper slurper = new JsonSlurper()
    Map item_data = slurper.parseText(lookup_proc.text)

    //println "raw item data: ${item_data}"

    this.itemCache[itemName] = item_data
    return item_data
}

def fieldValue(Map credential, String field, String defaultValue = "") {
    List<Map> onepass_mappings = credential.get('onepass', [])
    Map fieldMapping = onepass_mappings.find { it.target == field }

    // no lookup defined, return direct field value
    if (fieldMapping == null) {
        println "no field mapping for '${field}'"
        return credential.get(field, defaultValue)
    }
    println "field mapping for '${field}': ${fieldMapping}"
    // return lookup from 1pass
    def lookupVal =  lookup(fieldMapping.item, fieldMapping.vault, fieldMapping.section, fieldMapping.field)
    return lookupVal ? lookupVal : defaultValue

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

            Credentials cred = null
            switch(credential.type) {
                case 'usernamepassword':
                    credList.add(new UsernamePasswordCredentialsImpl(CredentialsScope.valueOf(credential.scope), credential.id, credential.description, fieldValue(credential, "username"), fieldValue(credential, "password")))
                    break

                case 'sshprivatekey':
                    def rawPrivatekey = fieldValue(credential, "privatekey")

                    // 1Password specific workaround for single-line secret store
                    def sanePrivatekey = rawPrivatekey.replace(' RSA PRIVATE ', 'RSAPRIVATE').replace(' ', '\n').replace('RSAPRIVATE', ' RSA PRIVATE ')
                    def keySource = new BasicSSHUserPrivateKey.DirectEntryPrivateKeySource(sanePrivatekey)
                    def username = fieldValue(credential, "username")
                    def password = fieldValue(credential, "password")
                    credList.add(new BasicSSHUserPrivateKey(CredentialsScope.valueOf(credential.scope), credential.id, username, keySource, password, credential.description ))
                    break

                case 'string':
                    Secret secret = Secret.fromString(fieldValue(credential, "secret"))
                    credList.add(new StringCredentialsImpl(CredentialsScope.valueOf(credential.scope), credential.id, credential.description, secret))
                    break

                case 'file':
                    byte[] plainText = (credential.path as File).bytes
                    SecretBytes secret = new SecretBytes(false, plainText)
                    credList.add(new FileCredentialsImpl(CredentialsScope.valueOf(credential.scope), credential.id, credential.description, credential.filename, secret))
                    break

                default:
                    println "WARNING: don't know how to handle credentials of type '${credential.type}', skipping."

            }
            //credentialStore.addCredentials(domain, cred)

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

println "consuming 1pass service credentials"

// jenkins should not allow access to these in builds, otherwise we are in trouble
// username, password, domain, masterkey
this.itemCache = [:]
this.opToken = null

Yaml yamlParser = new Yaml()
Map opBoot = yamlParser.load(("${System.getProperty('user.home')}/onepass_boot.yaml" as File).text)

// String op_token = this.signin(System.getenv("JENKINS_ONEPASS_DOMAIN"), System.getenv("JENKINS_ONEPASS_USERNAME"), System.getenv("JENKINS_ONEPASS_PASSWORD"), System.getenv("JENKINS_ONEPASS_MASTERKEY"))
String op_token = this.signin(opBoot.onepass_domain, opBoot.onepass_email, opBoot.onepass_password, opBoot.onepass_secret_key)
if (this.opToken) {
    println "1Password signin successful."
}
else {
    println "1Password signin failed, bailing out."
    return
}

String configFilePath = "${System.getProperty('user.home')}/initial_credentials.yaml"

File credentialsFile = new File(configFilePath)
if(!credentialsFile.exists()) {
    println "no such file: ${configFilePath} will not try to do setup"
    return
}
println "using credentials file in ${configFilePath}"

Map credentialsConfig = yamlParser.load((configFilePath as File).text)

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
