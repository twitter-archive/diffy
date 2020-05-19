package com.twitter.diffy.lifter

import com.twitter.concurrent.NamedPoolThreadFactory
import com.twitter.util.{ExecutorServiceFuturePool, Future, FuturePool}
import java.util.concurrent.{ArrayBlockingQueue, ThreadPoolExecutor, TimeUnit}

case class Message(endpoint: Option[String], result: FieldMap[Any])

trait MapLifter {
  def apply(input: Array[Byte]): Future[Message]
}

object MapLifterPool {
  val QueueSizeDefault = 5

  def apply(mapLifterFactory: => MapLifter) = {
    val executorService =
      new ThreadPoolExecutor(
        3,   // core pool size
        10,  // max pool size
        500, // keep alive time
        TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue[Runnable](10), // work queue
        new NamedPoolThreadFactory("maplifter", makeDaemons = true),
        new ThreadPoolExecutor.AbortPolicy()
      )
    executorService.prestartCoreThread()
    new MapLifterPool(mapLifterFactory, new ExecutorServiceFuturePool(executorService))
  }
}

class MapLifterPool(underlying: MapLifter, futurePool: FuturePool) extends MapLifter {
  override def apply(input: Array[Byte]): Future[Message] =
    (futurePool { underlying(input) }).flatten
}

