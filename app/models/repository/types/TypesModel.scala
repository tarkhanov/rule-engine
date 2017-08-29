package models.repository.types

import scala.util.matching.Regex

object TypesModel {

  case class Type(seq: Option[String], name: String, fields: List[Field])

  case class Field(name: String, typeDef: String)

  val builtInTypes = List("int", "string")

  object TypeDefs {
    val isBasicType: Regex = "(int|string|anyType)".r
    val isSequence: Regex = "^(?:sequence|seq):([^;]*?);?$".r
    val isIdentifier: Regex = "^id:(.*)$".r
    val isList: Regex = "^list\\[(.*)\\]$".r
  }
}
