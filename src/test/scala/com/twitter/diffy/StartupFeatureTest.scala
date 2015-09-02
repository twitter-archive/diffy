package com.twitter.diffy

import com.google.inject.Stage
import com.twitter.finatra.http.test.EmbeddedHttpServer
import com.twitter.inject.Test

class StartupFeatureTest extends Test {

  val server = new EmbeddedHttpServer(
    stage = Stage.PRODUCTION,
    twitterServer = new MainService {
    },
    extraArgs = Seq(
      "-proxy.port=:9992",
      "-candidate=localhost:80",
      "-master.primary=localhost:80",
      "-master.secondary=localhost:80",
      "-service.protocol=http"))

  "verify startup" in {
    server.assertHealthy()
  }
}
