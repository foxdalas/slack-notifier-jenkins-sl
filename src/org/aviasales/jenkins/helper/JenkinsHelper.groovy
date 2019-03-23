package org.aviasales.jenkins.helper

def BuildContainer(image, dockerFile, dir) {
  container('docker') {
    ansiColor('xterm') {
      def dockerImage = docker.build(image, "-f ${dockerFile} ${dir}")
      retry(5) {
        dockerImage.push("${env.BRANCH_NAME}-${env.BUILD_NUMBER}")
        if (env.BRANCH_NAME == 'master') {
          dockerImage.push("stable")
        }
      }
    }
  }
}

def BuildContainerKaniko(image, dockerFile, dir) {
  container(name: 'kaniko', shell: '/busybox/sh') {
    ansiColor('xterm') {
      withEnv(['PATH+EXTRA=/busybox:/kaniko']) {
        sh """#!/busybox/sh
        executor -f ${dockerFile} -c . --no-push
        """    
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
  def changeLogSets = currentBuild.rawBuild.changeSets
  for (int i = 0; i < changeLogSets.size(); i++) {
      def entries = changeLogSets[i].items
      for (int j = 0; j < entries.length; j++) {
          def entry = entries[j]
          echo "${entry.commitId} by ${entry.author} on ${new Date(entry.timestamp)}: ${entry.msg}"
          def files = new ArrayList(entry.affectedFiles)
          for (int k = 0; k < files.size(); k++) {
              def file = files[k]
              echo "  ${file.editType.name} ${file.path}"
          }
      }
  }
}

def getLastSuccessfulCommit() {
  def lastSuccessfulHash = null
  def lastSuccessfulBuild = currentBuild.rawBuild.getPreviousSuccessfulBuild()
  if ( lastSuccessfulBuild ) {
    lastSuccessfulHash = commitHashForBuild(lastSuccessfulBuild)
  }
  return lastSuccessfulHash
}
