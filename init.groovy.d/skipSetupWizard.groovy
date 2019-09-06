import static jenkins.model.Jenkins.instance as jenkins
import jenkins.install.InstallState
println "SETUP WIZARD " + "============================================================================================="

if (!jenkins.installState.isSetupComplete()) {
  println 'disable setup wizard'
  InstallState.INITIAL_SETUP_COMPLETED.initializeState()
}

