package com.twitter.diffy.util

import java.util.Date

import com.twitter.diffy.ParentSpec
import com.twitter.logging.Logger
import com.twitter.util.Await
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class EmailSenderSpec extends ParentSpec {

  val log = mock[Logger]
  val sender = new EmailSender(log, _ => ())
  describe("EmailSender") {
    it("should not encouter any errors while trying to compose emails") {
      Await.result(
        sender(
          SimpleMessage(
            from = "Diffy <diffy@twitter.com>",
            to = "diffy-team@twitter.com",
            bcc = "diffy-team@twitter.com",
            subject = "Diffy Report at " + new Date,
            body = "just testing emails from mesos!"
          )
        )
      )
      verifyZeroInteractions(log)
    }
  }
}
