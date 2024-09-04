#!/usr/bin/env groovy

def call(credentialsId, image, dockerFile, dir) {
  container('docker') {
    ansiColor('xterm') {
      withCredentials([file(credentialsId: 'dockerhub', variable: 'SecretFile')]) {
        sh "mkdir -p /root/.docker"
        sh "cp $SecretFile ~/.docker/config.json"

        def dockerImageECR = docker.build("${env.JENKINS_GLOBAL_REGISTRY_URL}/${image}", "-f ${dockerFile} ${dir}")
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
