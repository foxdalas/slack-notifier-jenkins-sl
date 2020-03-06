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
    if (env.BRANCH_NAME == 'master') {
      println("[ DEBUG ] BRANCH_NAME == master, using default params...")
    } else {
      def slackDirect = new Boolean(env.SLACK_DIRECT)
      def slackDirectApiUrl = env.SLACK_DIRECT_API_URL
      if (slackDirect) {
        if (slackDirectApiUrl != null || slackDirectApiUrl.size() != 0) {
          JenkinsHelper helper = new JenkinsHelper()
          def slackUser = helper.getSlackUserByGithub(slackDirectApiUrl)
          if(slackUser != null || slackUser.size() != 0) {
            obj.channel = slackUser
          } else {
            println("[ DEBUG ] slackUser == null OR size is zero")
          }
        } else {
          println("[ DEBUG ] slackDirectApiUrl == null OR size is zero")
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
