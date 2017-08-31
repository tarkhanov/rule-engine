package controllers

import play.api.i18n.{I18nSupport, Lang}
import play.api.mvc.{ControllerComponents, InjectedController, Request}

import scala.concurrent.ExecutionContext

trait InternationalSupport {
  this: I18nSupport =>

  implicit def lang(implicit request: Request[_]): Lang = request.lang(messagesApi)

  protected def controllerComponents: ControllerComponents

  implicit def availableLanguages: Seq[Lang] = controllerComponents.langs.availables

}

trait InternationalInjectedController extends InjectedController with I18nSupport with InternationalSupport with RequestImplicits {

  implicit def executionContext: ExecutionContext = controllerComponents.executionContext

}