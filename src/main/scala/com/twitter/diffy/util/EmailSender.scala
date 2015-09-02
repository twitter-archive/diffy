package com.twitter.diffy.util

import com.twitter.logging.Logger
import com.twitter.util.{FuturePool, Future}

import javax.mail._
import javax.mail.internet.{InternetAddress, MimeMessage}
import java.util.Properties

case class SimpleMessage(
  from: String,
  to: String,
  bcc: String,
  subject: String,
  body: String)

class EmailSender(log: Logger, send: MimeMessage => Unit = Transport.send) {
  private[this] val props = new Properties
  props.put("mail.smtp.host", "localhost")
  props.put("mail.smtp.auth", "false")
  props.put("mail.smtp.port", "25")

  private[this] val session = Session.getDefaultInstance(props, null)

  def apply(msg: SimpleMessage): Future[Unit] =
    FuturePool.unboundedPool {
      val message = new MimeMessage(session)
      message.setFrom(new InternetAddress(msg.from))
      message.setRecipients(
        Message.RecipientType.TO,
        InternetAddress.parse(msg.to) map { _.asInstanceOf[Address]}
      )
      message.addRecipients(
        Message.RecipientType.BCC,
        InternetAddress.parse(msg.bcc) map { _.asInstanceOf[Address]}
      )
      message.setSubject(msg.subject)
      message.setContent(msg.body, "text/html; charset=utf-8")
      try {
        send(message)
      } catch { case e =>
        log.error(e, "failed to send message")
      }
    }
}
