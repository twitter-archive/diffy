package com.twitter.diffy.lifter

import com.twitter.util.Try

object StringLifter {
  val htmlRegexPattern = """<("[^"]*"|'[^']*'|[^'">])*>""".r

  def lift(string: String): Any = {
    Try(JsonLifter.lift(JsonLifter.decode(string))).getOrElse {
      if(htmlRegexPattern.findFirstIn(string).isDefined)
        HtmlLifter.lift(HtmlLifter.decode(string))
      else string
    }
  }
}
