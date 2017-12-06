package controllers

import javax.inject.Inject

import controllers.security.AuthAction
import play.api.i18n.I18nSupport
import play.api.mvc.InjectedController
import play.filters.csrf.CSRFCheck
import services.RepositoryService

import scala.concurrent.ExecutionContext

class SessionController @Inject()(authenticatedAction: AuthAction, repositoryService: RepositoryService)
                                 (implicit ex: ExecutionContext)
                                  extends InjectedController with I18nSupport with RequestImplicits {

  private val defaultRedirect = Redirect(Pages.defaultLandingPage)

  def commit = authenticatedAction async {
    implicit request =>
      repositoryService.commit(request.user.uid).map {
        _ => defaultRedirect
      }.recover {
        case ex: IllegalStateException => defaultRedirect withError ex
        case ex: IllegalArgumentException => defaultRedirect withError ex
      }
  }

  def cancel = authenticatedAction async {
    implicit request =>
      repositoryService.cancel(request.user.uid).map {
        _ => defaultRedirect
      }
  }

}
