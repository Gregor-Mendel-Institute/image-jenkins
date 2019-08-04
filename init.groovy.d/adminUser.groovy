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

