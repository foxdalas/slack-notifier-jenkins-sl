package org.aviasales.jenkins.helper

def BuildContainer(name, image, dockerFile) {
  return {
    stage("Build #{name}") {
      container('docker') {
        ansiColor('xterm') {
          def dockerImage = docker.build(image, "-f ${dockerFile} .")
          dockerImage.push("${env.BRANCH_NAME}-${env.BUILD_NUMBER}")
          if (env.BRANCH_NAME == 'master') {
            dockerImage.push("stable")
          }
        }
      }
    }
  }
}