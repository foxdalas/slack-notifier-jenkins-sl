#!/usr/bin/env groovy

def call(command) {
  script {
    catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
      sh command
    }
  }
}