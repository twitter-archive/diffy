package com.twitter.diffy.lifter

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements

import scala.collection.JavaConversions._

object HtmlLifter {
  def lift(node: Element): FieldMap[Any] = node match {
    case doc: Document =>
      FieldMap(
        Map(
          "head" -> lift(doc.head),
          "body" -> lift(doc.body)
        )
      )
    case doc: Element => {
      val children: Elements = doc.children
      val attributes =
        FieldMap[String](
          doc.attributes.asList map { attribute =>
            attribute.getKey -> attribute.getValue
          } toMap
        )

      FieldMap(
        Map(
          "tag"         -> doc.tagName,
          "text"        -> doc.ownText,
          "attributes"  -> attributes,
          "children"    -> children.map(element => lift(element))
        )
      )
    }
  }

  def decode(html: String): Document = Jsoup.parse(html)
}