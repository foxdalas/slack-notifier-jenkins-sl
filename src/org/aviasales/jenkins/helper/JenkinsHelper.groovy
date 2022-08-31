package org.aviasales.jenkins.helper

import hudson.FilePath
import java.security.MessageDigest
import java.io.*
import groovy.json.JsonSlurper

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

def BuildContainerDeclarative(image, dockerFile, dir) {
  stage(image) {
    container('docker') {
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
        executor -f ${dockerFile} --context=dir://${env.WORKSPACE}  --cache=true --cache-dir=/cache/docker -d hub.docker.com/aviasales/${image}/${env.BRANCH_NAME}-${env.BUILD_NUMBER} -v warn
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

def checkPrometheusAlerts(dir) {
  container('golang') {
    stage("Checking monitoring rules if exist") {
      def rules = findFiles(glob: "${dir}/*.yml")
      def rulesFiles = []
      rules.each { rule ->
        rulesFiles << rule.path
      }
      sh "promtool check rules ${rulesFiles.join(" ")}"
    }
  }
}

def getSubfolders(dir) {
  def subfolders = sh(returnStdout: true, script: "ls -d ${dir}/*").trim().split(System.getProperty("line.separator"))
  return subfolders
}

def getChecksum(path, type) {
  def file = new File (path)  
  def digest = MessageDigest.getInstance(type)
  def inputstream = file.newInputStream()
  def buffer = new byte[16384]
  def len

  while((len=inputstream.read(buffer)) > 0) {
    digest.update(buffer, 0, len)
  }
  inputstream.close();
  def sha1sum = digest.digest()

  def result = ""
  for(byte b : sha1sum) {
    result += toHex(b)
  }
  return result
}

def abortPreviousRunningBuilds() {
  def hi = Hudson.instance
  def pname = env.JOB_NAME.split('/')[0]

  hi.getItem(pname).getItem(env.JOB_BASE_NAME).getBuilds().each { build ->
    def exec = build.getExecutor()

    if (build.number != currentBuild.number && exec != null) {
      if (env.BRANCH_NAME != "master") {
        exec.interrupt(
            Result.ABORTED,
            new CauseOfInterruption.UserInterruption(
                "Aborted by #${currentBuild.number}"
            )
        )
        println("Aborted previous running build #${build.number}")
      } else {
        println("Master branch! Skip abort")
      }
    } else {
      println("Build is not running or is current build, not aborting - #${build.number}")
    }
  }
}

def catchUnstable(command) {
  script {
    catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
      sh command
    }
  }
}

def prepareCache(baseFile, source, destination) {
  script {
    def md5 = sh([ script: "md5sum ${baseFile} | awk '{print \$1}'", returnStdout: true ]).trim()
    def folder = new File( "${source}/${md5}")
    if(folder.exists() ) {
      sh "mkdir -p ${destination}"
      sh "ls -A -1 ${source}/${md5} | parallel --gnu -v -j20 rsync -arp ${source}/${md5}/{} ${destination}"
    }
  }
}

def getSlackSaloUserByEmail(email) {
  def userName = ''
  def sapogSimpleApiUrl = env.SAPOG_SIMPLE_URL
  try {
    def http = new URL("${sapogSimpleApiUrl}${email}").openConnection() as HttpURLConnection

    http.setRequestMethod('GET')
    http.setDoOutput(true)
    http.setRequestProperty("Accept", 'application/json')
    http.setRequestProperty("Content-Type", 'application/json')
    http.outputStream.write(body.getBytes("UTF-8"))
    http.connect()

    if (http.responseCode == 200) {
      def sapogResponse = new JsonSlurper().parseText(http.inputStream.getText('UTF-8'))
      if(sapogResponse.success) {
        userName = "${sapogResponse.data.name}";
      } else {
        println("[ getSlackSaloUserByEmail ] response message: ${sapogResponse.message}")
      }
    } else {
      println("[ getSlackSaloUserByEmail ] response code: ${http.responseCode}")
    }
  } catch (Exception e) {
    println("[ getSlackSaloUserByEmail ] exception: ${e}")
  }
  return userName
}

def getSlackSaloUserIdByEmail(email) {
  def userId = ''
  def sapogSimpleApiUrl = env.SAPOG_SIMPLE_URL
  try {
    def http = new URL("${sapogSimpleApiUrl}${email}").openConnection() as HttpURLConnection

    http.setRequestMethod('GET')
    http.setDoOutput(true)
    http.setRequestProperty("Accept", 'application/json')
    http.setRequestProperty("Content-Type", 'application/json')
    http.outputStream.write(body.getBytes("UTF-8"))
    http.connect()

    if (http.responseCode == 200) {
      def sapogResponse = new JsonSlurper().parseText(http.inputStream.getText('UTF-8'))
      if(sapogResponse.success) {
        userId = "${sapogResponse.data.user_id}";
      } else {
        println("[ getSlackSaloUserIdByEmail ] response message: ${sapogResponse.message}")
      }
    } else {
      println("[ getSlackSaloUserIdByEmail ] response code: ${http.responseCode}")
    }
  } catch (Exception e) {
    println("[ getSlackSaloUserIdByEmail ] exception: ${e}")
  }
  return userId
}