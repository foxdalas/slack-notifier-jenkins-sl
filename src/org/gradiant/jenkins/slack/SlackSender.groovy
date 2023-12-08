package org.gradiant.jenkins.slack


void send(String message, String color = '#fff') {
  def options = getOptions message, color
  slackSend options
}


def getOptions(String message = '', String color = '') {
  def obj = [
    message: message
  ]

  if (color) {
    obj.color = color
  }

  if (env.SLACK_CHANNEL) {
    obj.channel = env.SLACK_CHANNEL
  }

  if (env.SLACK_DIRECT) {
    if (env.BRANCH_NAME ==~ /(^master$|^release\/.+)/) {
      println("[ DEBUG ] BRANCH_NAME == master OR release*, using default params...")
    } else {
      def slackDirect = new Boolean(env.SLACK_DIRECT)
      if (slackDirect) {
        JenkinsHelper helper = new JenkinsHelper()
        def mention = helper.getSlackUserByGithub()
        if(mention != null || mention.size() != 0) {
          obj.channel = mention
        } else {
          println("[ DEBUG ] mention is empty")
        }
      } else {
        println("[ DEBUG ] env.SLACK_DIRECT false")
      }
    }
  } else {
    println("[ DEBUG ] env.SLACK_DIRECT not found")
  }

  if (env.SLACK_DOMAIN) {
    obj.teamDomain = env.SLACK_DOMAIN
  }

  if (env.SLACK_CREDENTIALS) {
    obj.tokenCredentialId = env.SLACK_CREDENTIALS
  }

  return obj
}
