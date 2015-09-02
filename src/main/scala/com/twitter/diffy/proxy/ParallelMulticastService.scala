package com.twitter.diffy.proxy

import com.twitter.finagle.Service
import com.twitter.util.{Future, Try}

class ParallelMulticastService[-A, +B](
    services: Seq[Service[A, B]])
  extends Service[A, Seq[Try[B]]]
{
  def apply(request: A): Future[Seq[Try[B]]] =
    Future.collect(services map { _(request).liftToTry })
}