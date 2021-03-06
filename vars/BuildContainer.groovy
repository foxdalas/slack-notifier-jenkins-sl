#!/usr/bin/env groovy

def call(credentialsId, image, dockerFile, dir) {
  container('docker') {
    ansiColor('xterm') {
      withDockerRegistry([credentialsId: credentialsId]) {
        def dockerImage = docker.build(image, "-f ${dockerFile} ${dir}")
        retry(5) {
          dockerImage.push("${env.BRANCH_NAME}-${env.BUILD_NUMBER}")
          if (env.BRANCH_NAME == 'master') {
            dockerImage.push("stable")
            dockerImage.push("latest")
          }
        }
      }
    }
  }
}