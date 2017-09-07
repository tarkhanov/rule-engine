package services.rules

import javax.inject.Inject

import models.repository.rules.RulesModel._
import models.repository.rules.RulesModelXML._
import models.repository.rules.{RuleRepositoryRec, RuleRepositoryRecDetails}
import persistence.repository.Repository
import services.execution.RulesPythonExecutor
import services.execution.RulesPythonExecutor.{BodyResult, ConditionResult, RequestDataType}
import services.rules.RulesService.RuleNotFound

import scala.concurrent.{ExecutionContext, Future}

object RulesService {
  class RuleNotFound(message: String) extends Exception(message)
}

class RulesService @Inject()(repository: Repository, rulesPythonExecutor: RulesPythonExecutor)(implicit ex: ExecutionContext) extends ConfigureRulesService {

  private val rulesRepository = repository.rulesRepository

  def create(user: String): Future[Long] = {
    val name = "New RuleSet"
    val definition = defaultRuleSet(name)
    val newRuleSet = RuleRepositoryRec(0L, "", "", name, serialize(definition).toString())
    rulesRepository.create(Some("new"), newRuleSet, user)
  }

  def getRecordDetails(id: Long, user: String): Future[Option[RuleRepositoryRecDetails]] =
    rulesRepository.getRecordDetails(id, user)

  def edit(id: Long, user: String) = {
    rulesRepository.get(id).flatMap {
      case Some(current) => rulesRepository.create(Some(current.seq), current, user)
      case None => Future.failed(new RuleNotFound("Rule not found"))
    }
  }

  def saveDefinition(id: Long, rulesDefinition: RuleSet) = {
    rulesRepository.saveDefinition(id, rulesDefinition)
  }

  def invoke(requestData: RequestDataType, ruleSetFilter: String): Future[List[(Rule, ConditionResult, Option[BodyResult])]] = {
    rulesRepository.lookup(ruleSetFilter).flatMap {
      case Some(rule) =>
        rulesPythonExecutor.execute(requestData, rule.id, rule.definition)
      case None =>
        Future.successful(List.empty)
    }
  }

  def lookup(ruleSetFilter: String): Future[Option[RuleRepositoryRec]] = {
    rulesRepository.lookup(ruleSetFilter)
  }

  def defaultRuleSet(name: String) = {
    RuleSet(
      None,
      name,
      Arguments(
        List(
          Argument("argument1", "string")
        )
      ),
      Results(
        List(
          Result("result1", "string")
        )
      ),
      Rules(
        List(
          Rule(
            Some("Example"),
            Condition("argument1 == \"OK\""),
            Body("result1 = \"Success\"")
          )
        )
      )
    )
  }

  override def create(newSeq: Option[String], newRec: RuleRepositoryRec, user: String): Future[Long] = {
    rulesRepository.create(newSeq, newRec, user)
  }

}
