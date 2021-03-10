#!/usr/bin/env groovy

def call() {
  def notifier = new org.gradiant.jenkins.slack.SlackNotifier()
  notifier.notifyStart()
}