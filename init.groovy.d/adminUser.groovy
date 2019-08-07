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
println "TOKEN TOKEN TOKEN ============================================="
println "write token for ${username} user"
// you can change the "admin" name
// the false is to explicitely ask to not create a user who does not exist yet
// def user = User.get("api_admin", true)
def user = User.getOrCreateByIdOrFullName(username)
def prop = user.getProperty(ApiTokenProperty.class)
// the name is up to you
def token = prop.tokenStore.generateNewToken("api-super-admin-token")
user.save()

File file = new File("${System.getProperty('user.home')}/api_admin_token.txt")
file.write "${token.plainValue}\n"
// write, then set permissions
file.setReadable(true, true)
file.setWritable(true, true)
file.setExecutable(false)

println "admin token done."

