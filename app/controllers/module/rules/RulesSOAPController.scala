package controllers.module.rules

import javax.inject.Inject

import com.typesafe.scalalogging.StrictLogging
import controllers.InternationalInjectedController
import models.repository.rules.RulesModel.Rule
import services.execution.RulesPythonExecutor._
import services.rules.RulesService

import scala.concurrent.Future
import scala.xml.{Elem, Node}

class RulesSOAPController @Inject()(rules: RulesService) extends InternationalInjectedController with StrictLogging {

  def invoke(ruleSetFilter: String) = Action.async(parse.tolerantXml) {
    request =>
      (request.body \\ "rulesRequest")
        .headOption
        .map(prepareSOAPRequest)
        .map(rules.invoke(_, ruleSetFilter))
        .getOrElse(Future.failed(new IllegalArgumentException("RuleSet not found")))
        .map(r => r.map(rule))
        .map(list => Ok(successEnvelope(list)).as("text/xml"))
        .recover {
          case ex: IllegalArgumentException =>
            logger.debug("Exception during RuleSet execution", ex)
            BadRequest(failureEnvelope(1, ex.getMessage)).as("text/xml")
          case ex: RuleExecutionException =>
            logger.debug("Exception during RuleSet execution", ex)
            BadRequest(failureEnvelope(2, ex.getMessage)).as("text/xml")
        }
  }

  private def isElem(node: Node): Boolean =
    node match {
      case _: Elem => true
      case _ => false
    }

  private def prepareSOAPRequest(node: Node): RequestDataType = {

    def process(nodes: scala.Seq[Node]): RequestDataType =
      nodes.groupBy(_.label).mapValues(_.map(item => {
        val elems = item.child.filter(isElem)
        if (elems.isEmpty)
          Left(item.text)
        else
          Right(process(elems))
      }).toList)

    process(node.child.filter(isElem))
  }

  private def rule(ruleRecord: (Rule, ConditionResult, Option[BodyResult])): Elem = {

    val (rule, condition, bodyOption) = ruleRecord

    val resultNodes = bodyOption.map {
      bodyResult => {
        bodyResult.value.map {
          case (name, value) => response(name, value)
        }.flatten
      }
    }.getOrElse(List.empty)

    <rule name={rule.name.orNull} condition={condition.value.toString}>
      {condition.exceptions.map(ex => <exception location="condition">
      {ex.getClass.getName + ": " + ex.getMessage}
    </exception>)}{bodyOption.map(_.exceptions.map { ex =>
      <exception location="body">
        {ex.getClass.getName + ": " + ex.getMessage}
      </exception>
    }).getOrElse(List.empty)}{resultNodes}
    </rule>
  }

  private def response(name: String, value: RuleResultType): List[Node] =
    value.map {
      case Left(v: String) =>
        <item>
          {v}
        </item>.copy(label = name)
      case Right(m: Map[String, AnyRef]) =>
        <item>
          {m.flatMap { case (n, v) => response(n, v.asInstanceOf[RuleResultType]) }.toList}
        </item>.copy(label = name)
    }

  private def envelope(content: Elem): Elem =
    <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:rul="http://onbpm.com/rules/">
      <soapenv:Header/>
      <soapenv:Body>
        {content}
      </soapenv:Body>
    </soapenv:Envelope>

  private def successEnvelope(content: List[Elem]): Elem =
    envelope(
      <rul:rulesResponse>
        {content}
      </rul:rulesResponse>
    )

  private def failureEnvelope(code: Int, message: String): Elem =
    envelope(
      <rul:rulesFault>
        <code>{code}</code>
        <message>{message}</message>
      </rul:rulesFault>
    )

}