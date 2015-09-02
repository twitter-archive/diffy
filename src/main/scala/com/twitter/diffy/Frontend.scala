package com.twitter.diffy

import javax.inject.Inject

import com.twitter.diffy.proxy.Settings
import com.twitter.finatra.http.Controller
import com.twitter.finagle.http.Request

class Frontend @Inject()(settings: Settings) extends Controller {

  case class Dashboard(
    serviceName: String,
    serviceClass: String,
    apiRoot: String,
    excludeNoise: Boolean,
    relativeThreshold: Double,
    absoluteThreshold: Double)

  get("/") { req: Request =>
    response.ok.view(
      "dashboard.mustache",
      Dashboard(
        settings.serviceName,
        settings.serviceClass,
        settings.apiRoot,
        req.params.getBooleanOrElse("exclude_noise", false),
        settings.relativeThreshold,
        settings.absoluteThreshold)
    )
  }

  get("/css/:*") { request: Request =>
    response.ok.file(request.path)
  }

  get("/scripts/:*") { request: Request =>
    response.ok.file(request.path)
  }
}