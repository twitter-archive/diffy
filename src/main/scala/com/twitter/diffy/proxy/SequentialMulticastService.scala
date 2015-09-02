package com.twitter.diffy.proxy

import com.twitter.finagle.Service
import com.twitter.util.{Future, Try}

class SequentialMulticastService[-A, +B](
    services: Seq[Service[A, B]])
  extends Service[A, Seq[Try[B]]]
{
  def apply(request: A): Future[Seq[Try[B]]] =
    services.foldLeft[Future[Seq[Try[B]]]](Future.Nil){ case (acc, service) =>
      acc flatMap { responseTries =>
        val nextResponse = service(request).liftToTry
        nextResponse map { responseTry => responseTries ++ Seq(responseTry) }
      }
    }
}
