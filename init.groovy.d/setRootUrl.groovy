import jenkins.model.JenkinsLocationConfiguration
jlc = new jenkins.model.JenkinsLocationConfiguration()
jlc.setUrl("http://localhost:8080/") 
println(jlc.getUrl())


