package models.repository.rules

import models.repository.rules.RulesModel._
import play.api.libs.json.{Format, JsResult, JsValue, Json}

object RulesModelJson {

  implicit val argumentFormat: Format[Argument] = Json.format[Argument]
  implicit val argumentsFormat: Format[Arguments] = new Format[Arguments] {
    override def reads(json: JsValue): JsResult[Arguments] = Json.fromJson[List[Argument]](json).map(list => Arguments(list))
    override def writes(item: Arguments): JsValue = Json.toJson(item.list)
  }

  implicit val resultFormat: Format[Result] = Json.format[Result]
  implicit val resultsFormat: Format[Results] = new Format[Results] {
    override def reads(json: JsValue): JsResult[Results] = Json.fromJson[List[Result]](json).map(list => Results(list))
    override def writes(item: Results): JsValue = Json.toJson(item.list)
  }
  implicit val conditionFormat: Format[Condition] = new Format[Condition] {
    override def reads(json: JsValue): JsResult[Condition] = Json.fromJson[String](json).map(code => Condition(code))
    override def writes(condition: Condition): JsValue = Json.toJson(condition.code)
  }
  implicit val bodyFormat: Format[Body] = new Format[Body] {
    override def reads(json: JsValue): JsResult[Body] = Json.fromJson[String](json).map(code => Body(code))
    override def writes(body: Body): JsValue = Json.toJson(body.code)
  }
  implicit val ruleFormat: Format[Rule] = Json.format[Rule]
  implicit val rulesFormat: Format[Rules] = new Format[Rules] {
    override def reads(json: JsValue): JsResult[Rules] = Json.fromJson[List[Rule]](json).map(list => Rules(list))
    override def writes(item: Rules): JsValue = Json.toJson(item.list)
  }
  implicit val ruleSetFormat: Format[RuleSet] = Json.format[RuleSet]

}
