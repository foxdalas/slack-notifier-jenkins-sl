package org.gradiant.jenkins.slack


String format(String title = '', String message = '', String testSummary = '') {
  def helper = new JenkinsHelper()

  def project = helper.getProjectName()
  def branch = helper.getBranchName()
  def buildNumber = helper.getBuildNumber()
  def url = ""
  if(helper.getBlueOceanLink()) {
    url = env.RUN_DISPLAY_URL
  }
  else {
    url = helper.getAbsoluteUrl()
  }

  def result = "${project}: ${branch} - №${buildNumber} ${title.trim()} (<${url}|Open>)"
  if(message) result = result + "\nChanges:\n\t ${message.trim()}"
  if(testSummary) result = result + "\n ${testSummary}"

  return result
}
