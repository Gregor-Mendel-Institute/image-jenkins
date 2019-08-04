# try it out:
docker build -t myjenkins .
docker run -p 8080:8080 -v /Users/ebirn/projects/jenkins-stateless/jenkins_home:/var/jenkins_home --name jenkins  -e ADMIN_USERNAME=admin -e ADMIN_PASSWORD=admin --rm  myjenkins


#
take upstream jenkins:

https://hub.docker.com/r/jenkins/jenkins/tags

# get installed plugins
Jenkins.instance.pluginManager.plugins.each{
  plugin ->
    println ("${plugin.getDisplayName()} (${plugin.getShortName()}): ${plugin.getVersion()}")
}


# ======= CREDENTIALS ===============
# all examples: https://www.greenreedtech.com/creating-jenkins-credentials-via-the-rest-api/
# see https://stackoverflow.com/questions/29616660/how-to-create-jenkins-credentials-via-the-rest-api
curl -X POST 'http://user:token@jenkins_server:8080/credentials/store/system/domain/_/createCredentials' \
--data-urlencode 'json={
  "": "0",
  "credentials": {
    "scope": "GLOBAL",
    "id": "identification",
    "username": "manu",
    "password": "bar",
    "description": "linda",
    "$class": "com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl"
  }
}'

# ssh keys
CRUMB=$(curl -s 'http://{{jenkins_admin_username}}:{{jenkins_admin_password}}@localhost:8080/crumbIssuer/api/xml?xpath=concat(//crumbRequestField,":",//crumb)')
curl -H $CRUMB -X POST 'http://{{jenkins_admin_username}}:{{jenkins_admin_password}}@localhost:8080/credentials/store/system/domain/_/createCredentials' \
--data-urlencode 'json={
  "": "0",
  "credentials": {
    "scope": "GLOBAL",
    "id": "'{{ii.ssh_user}}'",
    "username": "'{{ii.ssh_user}}'",
    "password": "",
    "privateKeySource": {
      "stapler-class": "com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey$FileOnMasterPrivateKeySource",
      "privateKeyFile": "'{{jenkins_home}}/{{ii.key_name}}.pem'",
    },
    "description": "'{{ii.ssh_user}}'",
    "stapler-class": "com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey"
  }
}'

....
"stapler-class": "com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey$DirectEntryPrivateKeySource",
      "privateKey": "{{private_key_content}}",
    },
    "description": "{{user}}",
    "stapler-class": "com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey"
...
# ====================================

