package controllers

import play.api.i18n.{I18nSupport, Lang}
import play.api.mvc.{InjectedController, Request}

import scala.concurrent.ExecutionContext

trait InternationalSupport {
  this: I18nSupport =>

  implicit def lang(implicit request: Request[_]): Lang = request.lang(messagesApi)

}

trait InternationalInjectedController extends InjectedController with I18nSupport with InternationalSupport with RequestImplicits {

  implicit def availableLanguages: Seq[Lang] = controllerComponents.langs.availables

  implicit def executionContext: ExecutionContext = controllerComponents.executionContext

}