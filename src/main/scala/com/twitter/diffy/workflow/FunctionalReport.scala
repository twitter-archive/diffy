package com.twitter.diffy.workflow

import javax.inject.Inject

import com.twitter.diffy.proxy.Settings
import com.twitter.finagle.util.DefaultTimer
import com.twitter.util.{Timer, Time}

class FunctionalReport @Inject()(
    settings: Settings,
    reportGenerator: ReportGenerator,
    override val timer: Timer = DefaultTimer.twitter)
  extends Workflow
{
  val delay = settings.emailDelay

  override def run(start: Time) = {
    log.info(s"Sending FunctionalReport at ${Time.now}")
    reportGenerator.sendEmail
  }
}