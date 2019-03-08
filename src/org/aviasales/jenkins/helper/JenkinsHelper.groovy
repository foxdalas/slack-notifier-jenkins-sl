package org.aviasales.jenkins.helper

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