package com.twitter.diffy.lifter

import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.twitter.util.{Try, NoStacktrace}

import scala.collection.JavaConversions._
import scala.language.postfixOps
import scala.reflect.runtime.universe.runtimeMirror
import scala.tools.reflect.ToolBox

object JsonLifter {
  object JsonNull
  object JsonParseError extends Exception with NoStacktrace

  val toolbox = runtimeMirror(getClass.getClassLoader).mkToolBox()

  val Mapper = new ObjectMapper with ScalaObjectMapper
  Mapper.registerModule(DefaultScalaModule)
  def apply(obj: Any): JsonNode = Mapper.valueToTree(obj)

  def lift(node: JsonNode): Any = node.asToken match {
    case JsonToken.START_ARRAY        =>
      node.elements.toSeq.map {
        element => lift(element)
      }  sortBy { _.toString }
    case JsonToken.START_OBJECT       => {
      val fields = node.fieldNames.toSet
      if (fields.exists{ field => Try(toolbox.parse(s"object ${field}123")).isThrow}) {
        node.fields map {field => (field.getKey -> lift(field.getValue))} toMap
      } else {
        FieldMap(
          node.fields map {field => (field.getKey -> lift(field.getValue))} toMap
        )
      }
    }
    case JsonToken.VALUE_FALSE        => false
    case JsonToken.VALUE_NULL         => JsonNull
    case JsonToken.VALUE_NUMBER_FLOAT => node.asDouble
    case JsonToken.VALUE_NUMBER_INT   => node.asLong
    case JsonToken.VALUE_TRUE         => true
    case JsonToken.VALUE_STRING       => node.textValue
    case _                            => throw JsonParseError
  }

  def decode(json: String): JsonNode = Mapper.readTree(json)
  def encode(item: Any): String = Mapper.writer.writeValueAsString(item)
}
