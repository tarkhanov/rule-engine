package controllers.module.rules

import javax.inject.Inject

import controllers.InternationalInjectedController
import controllers.security.AuthAction
import models.repository.rules.RulesModel._
import models.repository.rules.RulesModelXML
import persistence.repository.Repository
import play.api.libs.json._
import services.rules.RulesService
import services.rules.RulesService.RuleNotFound

import scala.concurrent.Future

class RulesController @Inject()(authenticatedAction: AuthAction, rules: RulesService) extends InternationalInjectedController {

  def create = authenticatedAction async {
    implicit request =>
      rules.create(request.user).map {
        rec => Redirect(routes.RulesController.open(rec))
      }
  }

  def open(id: Long) = authenticatedAction async {
    implicit request =>
      rules.getRecordDetails(id, request.user).map {
        case Some(rec) =>
          val readOnly = !rec.actions.exists(_.action == Repository.CREATE_ACTION)
          val definition = RulesModelXML.parse(rec.record.definition)
          Ok(views.html.module.rules.open(request.user, readOnly, rec, definition, request.log))
        case None =>
          NotFound(views.html.error.http404(request.user))
      }
  }

  def edit(id: Long) = authenticatedAction async {
    implicit request =>
      rules.edit(id, request.user).map {
        rec => Redirect(routes.RulesController.open(rec))
      }.recover {
        case _: RuleNotFound => NotFound(views.html.error.http404(request.user))
      }
  }

  def save(id: Long) = authenticatedAction.async(parse.json) {
    request =>
      request.body.result match {
        case JsDefined(jsonTypeDefinition) =>
          rules.saveDefinition(id, jsonDeserializeRuleSet(jsonTypeDefinition)).map {
            _ =>
              val response = JsObject(Seq("status" -> JsString("OK")))
              Ok(response).as("application/json")
          }.recover {
            case ex =>
              val response = JsObject(Seq("status" -> JsString("ERROR"), "error" -> JsString(ex.getMessage)))
              Ok(response).as("application/json")
          }
        case _ =>
          Future.successful(BadRequest)
      }
  }

  import models.repository.rules.RulesModelJson._

  // TODO: Validate JSON
  def jsonDeserializeRuleSet(json: JsValue): RuleSet = json.as[RuleSet]

}
