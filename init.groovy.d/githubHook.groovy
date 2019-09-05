// setup Github webhooks for repo discovery et al
// cannot do this via config as code, see
// FIXME hookUrl cannot be set here see https://github.com/jenkinsci/configuration-as-code-plugin/issues/672
// see also https://github.com/jenkinsci/github-plugin/pull/210
// see also unclassified.yaml

import java.net.URL
import jenkins.model.*
import org.jenkinsci.plugins.github.config.*
import org.jenkinsci.plugins.github.config.GitHubPluginConfig
def instance = Jenkins.getInstance()

println "GITHUB HOOK URL " + "========================================================================================="

def githubSettings = Jenkins.instance.getExtensionList(GitHubPluginConfig.class)[0]

def hookUrl = System.getenv("JENKINS_GITHUB_WEBOOK_URL")
println "setting url to ${hookUrl}"

githubSettings.overrideHookUrl = true
githubSettings.hookUrl = new URL(hookUrl)

