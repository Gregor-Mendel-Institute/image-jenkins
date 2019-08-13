/*
 * Create an admin user. 
 * FIXME this might be conflicting with what is coming from config as code setup
 */
// import jenkins.model.*
// import hudson.security.*
// 
// println "--> creating admin user"
// 
// def jenkins = Jenkins.getInstance()
// 
// def adminUsername = System.getenv("ADMIN_USERNAME")
// def adminPassword = System.getenv("ADMIN_PASSWORD")
// assert adminPassword != null : "No ADMIN_USERNAME env var provided, but required"
// assert adminPassword != null : "No ADMIN_PASSWORD env var provided, but required"
// 
// def securityRealm = jenkins.getSecurityRealm()
// // new HudsonPrivateSecurityRealm(false)
// securityRealm.createAccount(adminUsername, adminPassword)
// //jenkins.setSecurityRealm(hudsonRealm)
// 
// // ProjectMatrixAuthorizationStrategy
// def strategy = jenkins.getAuthorizationStrategy()
// if ((strategy instanceof ProjectMatrixAuthorizationStrategy) == false) {
//   println "adminUser script: setting ProjectMatrixAuthorizationStrategy "
//   strategy = new ProjectMatrixAuthorizationStrategy()
//   jenkins.setAuthorizationStrategy(strategy)
// }
// // admin we just created should have admin privs
// strategy.add(Jenkins.ADMINISTER, adminUsername)
// 
// jenkins.save()

import hudson.model.*
import jenkins.model.*
import jenkins.security.*
import jenkins.security.apitoken.*

def username = "jenkins_api_admin"
println "ADMIN TOKEN " + "=============================================================================================="
println "write token for ${username} user"
// you can change the "admin" name
// the false is to explicitely ask to not create a user who does not exist yet
// def user = User.get("api_admin", true)
def apiAdminUser = User.getOrCreateByIdOrFullName(username)
def prop = apiAdminUser.getProperty(ApiTokenProperty.class)

String tokenFilePath = "${System.getProperty('user.home')}/api_admin_token.txt"
File tokenFile = new File(tokenFilePath)
def existingToken = null
if (tokenFile.exists() && tokenFile.canRead()) {
    existingToken = tokenFile.readLines().first().trim()
    println "found existing token in ${tokenFilePath}"
}

// the name is up to you
// FIXME this will add a new token on every restart, but never update/remove

def tokenName = "api-super-admin-token"
def tokenStore = prop.tokenStore

// we found an existing token earlier, we might be able to re-use it
if (existingToken) {
    ApiTokenStore.HashedToken matchingToken = tokenStore.findMatchingToken(existingToken)
    if (matchingToken) {
        println "admin token '${matchingToken.name}' [uuid=${matchingToken.uuid}] already there. nothing to do, we can keep using it."
        return
    }
    else {
        println "existing token ${existingToken} could not be matched to any user tokens."
    }
}

println "generating new token for ${username} ..."
// generate new Token
def token = tokenStore.generateNewToken(tokenName)
apiAdminUser.save()

// write, then set permissions
tokenFile.setReadable(true, true)
tokenFile.setWritable(true, true)
tokenFile.setExecutable(false)

// write token to file
tokenFile.write "${token.plainValue}\n"

println "admin token done."
