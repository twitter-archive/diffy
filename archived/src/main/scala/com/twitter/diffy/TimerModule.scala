package com.twitter.diffy

import javax.inject.Singleton

import com.google.inject.Provides
import com.twitter.finagle.util.DefaultTimer
import com.twitter.inject.TwitterModule
import com.twitter.util.Timer

object TimerModule extends TwitterModule {
  @Singleton
  @Provides
  def providesTimer: Timer = DefaultTimer.twitter
}
