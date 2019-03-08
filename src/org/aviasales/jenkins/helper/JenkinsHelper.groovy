package org.aviasales.jenkins.helper

import hudson.plugins.accurev.*

def BuildContainer(image, dockerFile, dir) {
  container('docker') {
    ansiColor('xterm') {
      def dockerImage = docker.build(image, "-f ${dockerFile} ${dir}")
      dockerImage.push("${env.BRANCH_NAME}-${env.BUILD_NUMBER}")
      if (env.BRANCH_NAME == 'master') {
        dockerImage.push("stable")
      }
    }
  }
}

def PrepareGoPath(repoPrefix, projectName) {
  return {
    sh """
      mkdir -p /go/src/${repoPrefix}
      ln -sf ${env.WORKSPACE} /go/src/${repoPrefix}/${projectName}
    """
  }
}

def getFileChanges() {
    def changes = "Changes: \n"
    build = currentBuild
    // Go through the previous builds and get changes until the
    // last successful build is found.
    while (build != null && build.result != 'SUCCESS') {
        changes += "Build ${build.id}:\n"

        for (changeLog in build.changeSets) {
            for (AccurevTransaction entry in changeLog.items) {
                changes += "\n    Issue: " + entry.getIssueNum()
                changes += "\n    Change Type: " + entry.getAction()
                changes += "\n    Change Message: " + entry.getMsg()
                changes += "\n    Author: " + entry.getAuthor()
                changes += "\n    Date: " + entry.getDate()
                changes += "\n    Files: "
                for (path in entry.getAffectedPaths()) {
                    changes += "\n        " + path;
                }
                changes += "\n"
            }
        }
        build = build.previousBuild
    }
    echo changes
}