#!/usr/bin/env groovy

def call(message) {
  def notifier = new org.gradiant.jenkins.slack.SlackNotifier()
  notifier.notifyStringError("BUILD FAILED")
}