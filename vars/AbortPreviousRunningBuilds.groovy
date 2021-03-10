#!/usr/bin/env groovy

def call() {
  def hi = Hudson.instance
  def pname = env.JOB_NAME.split('/')[0]

  hi.getItem(pname).getItem(env.JOB_BASE_NAME).getBuilds().each { build ->
    def exec = build.getExecutor()

    if (build.number != currentBuild.number && exec != null) {
      if (env.BRANCH_NAME != "master") {
        exec.interrupt(
            Result.ABORTED,
            new CauseOfInterruption.UserInterruption("Aborted by #${currentBuild.number}")
        )
      }
    }
  }
}