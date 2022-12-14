#!/usr/bin/env groovy

def call(credentialsId, image, dockerFile, dir) {
  container('docker') {
    ansiColor('xterm') {
      withCredentials([file(credentialsId: 'dockerhub', variable: 'SecretFile')]) {
        sh "mkdir -p /root/.docker"
        sh "cp $SecretFile ~/.docker/config.json"

        def dockerImage = docker.build(image, "-f ${dockerFile} ${dir}")
        retry(5) {
          dockerImage.push("${env.BRANCH_NAME}-${env.BUILD_NUMBER}")
          if (env.BRANCH_NAME == 'master') {
            dockerImage.push("stable")
            dockerImage.push("latest")
          }
        }
        def dockerImageECR = docker.build("667601112203.dkr.ecr.us-east-2.amazonaws.com/${image}", "-f ${dockerFile} ${dir}")
        retry(5) {
          dockerImageECR.push("${env.BRANCH_NAME}-${env.BUILD_NUMBER}")
          if (env.BRANCH_NAME == 'master') {
            dockerImageECR.push("stable")
            dockerImageECR.push("latest")
          }
        }
      }
    }
  }
}