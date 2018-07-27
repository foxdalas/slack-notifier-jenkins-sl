package org.gradiant.jenkins.slack

import hudson.FilePath;

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

String getChangelog() {
  def messages = []
  def changeLogSets = currentBuild.rawBuild.changeSets
  for (int i = 0; i < changeLogSets.size(); i++) {
    def entries = changeLogSets[i].items
    for (int j = 0; j < entries.length; j++) {
      def entry = entries[j]
      messages << "${entry.commitId} by ${entry.author} on ${new Date(entry.timestamp)}: ${entry.msg}"
    }
  }
  return messages.join("\n")
}

