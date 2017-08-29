package models.repository.rules

object RulesModel {

  case class Argument(name: String, `type`: String)

  case class Arguments(list: List[Argument] = List())

  case class Result(name: String, `type`: String)

  case class Results(list: List[Result] = List())

  case class Condition(code: String)

  case class Body(code: String)

  case class Rule(name: Option[String], condition: Condition, body: Body)

  case class Rules(list: List[Rule] = List())

  case class RuleSet(seq: Option[String], name: String, arguments: Arguments, results: Results, rules: Rules)

}
