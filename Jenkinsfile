// see docs https://confluence.imp.ac.at/display/~erich.birngruber/VBC+CICD+tooling

def towerJobs = [
// we cannot auto-deploy to production as we would kill ourselves
//  tags:          [jobName:"App Jenkins Prod", jobTags: "reload", extraVars: "app_jenkins_container_tag: latest"],
  develop:       [jobName:"App Jenkins DEV", jobTags: "reload", extraVars: "app_jenkins_container_tag: develop"],
  update_maintenance: [jobName:"App Jenkins DEV", jobTags: "reload", extraVars: "app_jenkins_container_tag: update_maintenance"]
]

def extraBranches = ["develop", "update_maintenance"]

buildDockerImage([imageName: "jenkins", pushBranches: extraBranches, tower: towerJobs])

