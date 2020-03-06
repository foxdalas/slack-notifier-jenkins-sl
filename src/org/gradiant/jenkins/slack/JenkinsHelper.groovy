package org.gradiant.jenkins.slack

import hudson.FilePath
import groovy.json.JsonSlurper

String getBranchName() {
  return env.BRANCH_NAME
}


int getBuildNumber() {
  return currentBuild.number
}

Boolean getBlueOceanLink() {
  if(env.BLUE_OCEAN) return new Boolean(env.BLUE_OCEAN)
  return true
}

String getAbsoluteUrl() {
  return currentBuild.absoluteUrl
}


String getProjectName() {
  return env.BUILD_URL.split('job/')[1].split('/')[0]
}


List<String> getChanges() {
  List<String> messages = []
  for (int i = 0; i < currentBuild.changeSets.size(); i++) {
    def entries = currentBuild.changeSets[i].items
    for (int j = 0; j < entries.length; j++) {
      def entry = entries[j]
      messages.add("\t- ${entry.msg} [${entry.author}]")
    }
  }

  return messages
}

String getDuration() {
  return currentBuild.durationString.replace(' and counting', '')
}


String getCurrentStatus() {
  return currentBuild.currentResult
}


String getPreviousStatus() {
  def prev = currentBuild.previousBuild?.currentResult

  if (!prev) {
    return 'SUCCESS'
  }

  return prev
}

String getBuildUser() {
  if (!currentBuild.rawBuild.getCause(Cause.UserIdCause)) {
    return "jenkins"
  }
  return currentBuild.rawBuild.getCause(Cause.UserIdCause).getUserId().split("@")[0]
}

String getSlackUserByGithub(sapogApiUrl) {
  def slackUser = ''
  try {
    def commit = sh(returnStdout: true, script: 'git rev-parse HEAD')
    def author = sh(returnStdout: true, script: "git --no-pager show -s --format='%an' ${commit}").trim()
    def body = "{\"username\": \"${author}\"}"
    def http = new URL("${sapogApiUrl}").openConnection() as HttpURLConnection

    http.setRequestMethod('POST')
    http.setDoOutput(true)
    http.setRequestProperty("Accept", 'application/json')
    http.setRequestProperty("Content-Type", 'application/json')
    http.outputStream.write(body.getBytes("UTF-8"))
    http.connect()

    if (http.responseCode == 200) {
      def sapog = new JsonSlurper().parseText(http.inputStream.getText('UTF-8'))
      if(sapog.success) {
        slackUser = "@${sapog.data.slack}";
      } else {
        println("[ DEBUG ] response message: ${sapog.message}")
      }
    } else {
      println("[ DEBUG ] response code: ${http.responseCode}")
    }
  } catch (Exception e) {
    println("[ DEBUG ] getSlackUserByGithub exception: ${e}")
  }
  return slackUser
}

String getChangelog() {
  def messages = []
  def changeLogSets = currentBuild.rawBuild.changeSets
  for (int i = 0; i < changeLogSets.size(); i++) {
    def entries = changeLogSets[i].items
    for (int j = 0; j < entries.length; j++) {
      def entry = entries[j]
      messages << "${j+1}: ${entry.msg} ${entry.author}"
    }
  }
  return messages.join("\n")
}
