# try it out:
docker build -t myjenkins .
docker run -p 8080:8080 -v /Users/ebirn/projects/jenkins-stateless/jenkins_home:/var/jenkins_home --name jenkins  -e ADMIN_USERNAME=admin -e ADMIN_PASSWORD=admin --rm  myjenkins

docker run -p 8080:8080 -v $HOME/projects/image-jenkins/jenkins_home:/var/jenkins_home --name jenkins  -e ADMIN_USERNAME=admin -e ADMIN_PASSWORD=admin --rm  docker.artifactory.imp.ac.at/it/jenkins:initial_setup


# ===  create node via API ===
https://gist.github.com/Evildethow/be4614ba27882d8f4627a972a624d525



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

# ===================================================================
# ===================================================================
# ===================================================================

# ### Bitbucket pre setup
# https://docs.atlassian.com/bitbucket-server/rest/6.5.1/bitbucket-rest.html#idp129
# create project:
POST /rest/api/1.0/projects
{
    "key": "PRJ",
    "name": "My Cool Project",
    "description": "The description for my cool project.",
    "avatar": "data:image/png;base64,<base64-encoded-image-data>"
}

# authorize group
# https://docs.atlassian.com/bitbucket-server/rest/6.5.1/bitbucket-rest.html#idp60
PUT /REST/API/1.0/ADMIN/PERMISSIONS/GROUPS?PERMISSION&NAME

# give group project admin
# https://docs.atlassian.com/bitbucket-server/rest/6.5.1/bitbucket-rest.html#idp142
PUT /REST/API/1.0/PROJECTS/{PROJECTKEY}/PERMISSIONS/GROUPS?PERMISSION&NAME

# ### artifactory setup
# create local repository
https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API#ArtifactoryRESTAPI-CreateRepository
https://www.jfrog.com/confluence/display/RTF/Repository+Configuration+JSON

# add to virtual repository (docker registry)
https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API#ArtifactoryRESTAPI-UpdateRepositoryConfiguration
repo config: https://www.jfrog.com/confluence/display/RTF/Repository+Configuration+JSON#RepositoryConfigurationJSON-VirtualRepository


# grant group publish to group repository
# create group, link to ldap realm
https://www.jfrog.com/jira/browse/RTFACT-10537?focusedCommentId=41962&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-41962
# setup group permissions for local repo
https://www.jfrog.com/confluence/display/RTF/Security+Configuration+JSON#SecurityConfigurationJSON-application/vnd.org.jfrog.artifactory.security.PermissionTargetV2+json


# ### tower setup
# get all user members of group: https://docs.ansible.com/ansible/latest/modules/getent_module.html#getent-module
https://docs.ansible.com/ansible/latest/modules/tower_send_module.html#tower-send-module

# create team
https://docs.ansible.com/ansible/latest/modules/tower_team_module.html

# permit pipeline group to team
https://docs.ansible.com/ansible/latest/modules/tower_user_module.html#tower-user-module

# allow team to use inventory + TDE inventory
https://docs.ansible.com/ansible/latest/modules/tower_role_module.html#tower-role-module


# ??? host deployment allow svc group to sssd
# ??? host deployment allow svc group sudo podman logs, podman exec ?
# ??? add project discovery to jenkins




