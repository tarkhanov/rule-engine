package models.repository.rules

import java.io.StringReader
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.{SchemaFactory, Validator}

import models.repository.rules.RulesModel._

import scala.util.{Failure, Success, Try}
import scala.xml.{Elem, Node, XML}

/**
 * Created by Sergey Tarkhanov on 6/4/2015.
 */

object RulesModelXML {

  lazy val validator: Validator = {
    val schemaDefinition = getClass.getClassLoader.getResource("RuleSet.xsd")
    val schemaLang = "http://www.w3.org/2001/XMLSchema"
    val factory = SchemaFactory.newInstance(schemaLang)
    val schema = factory.newSchema(schemaDefinition)
    schema.newValidator()
  }

  def validate(xmlFile: String): Try[Boolean] =
    try {
      validator.validate(new StreamSource(new StringReader(xmlFile)))
      Success(true)
    } catch {
      case ex: Throwable => Failure(ex)
    }

  def parse(text: String): RuleSet = {
    val xmlDefinition = XML.load(new StringReader(text))
    parseDefinition(xmlDefinition)
  }

  private def parseArgument(xmlArgument: Node): Argument = {
    val name = xmlArgument \@ "name"
    val typeId = xmlArgument \@ "type"
    Argument(name, typeId)
  }

  private def parseArguments(xmlArguments: Node): Arguments = {
    val listOfArguments = (xmlArguments \\ "argument").map(parseArgument)
    Arguments(listOfArguments.toList)
  }


  private def parseResult(xmlArgument: Node): Result = {
    val name = xmlArgument \@ "name"
    val typeId = xmlArgument.attribute("type").map(_.head.text)
    Result(name, typeId.getOrElse("anyType"))
  }

  private def parseResults(xmlResults: Node): Results = {
    val listOfResults = (xmlResults \\ "result").map(parseResult)
    Results(listOfResults.toList)
  }

  private def parseRule(xmlRule: Node): Rule = {
    val name = xmlRule.attribute("name").map(_.head.text)
    val condition = Condition((xmlRule \ "condition").head.text)
    val result = Body((xmlRule \ "body").head.text)
    Rule(name, condition, result)
  }

  private def parseRules(xmlRules: Node): Rules = {
    val rules = (xmlRules \\ "rule").map(parseRule)
    Rules(rules.toList)
  }

  private def parseDefinition(xmlDefinition: Elem): RuleSet = {
    val seq = xmlDefinition.attribute("seq").map(_.head.text)
    val name = xmlDefinition.attribute("name").map(_.head.text).getOrElse("")
    val args = parseArguments((xmlDefinition \ "arguments").head)
    val results = (xmlDefinition \ "results").headOption.map(parseResults).getOrElse(Results(List.empty))
    val rules = parseRules((xmlDefinition \ "rules").head)
    RuleSet(seq, name, args, results, rules)
  }

  def serialize(ruleSet: RuleSet): Elem = {
    <RuleSet seq={ruleSet.seq.orNull} name={ruleSet.name}>
      <arguments>
        {ruleSet.arguments.list.map { arg => <argument name={arg.name} type={arg.`type`}/> }}
      </arguments>
      <results>
        {ruleSet.results.list.map(res => <result name={res.name} type={res.`type`}/>)}
      </results>
      <rules>
        {ruleSet.rules.list.map { rule => <rule name={rule.name.orNull}>
          <condition>{rule.condition.code}</condition>
          <body>{rule.body.code}</body>
        </rule> }}
      </rules>
    </RuleSet>
  }
}
