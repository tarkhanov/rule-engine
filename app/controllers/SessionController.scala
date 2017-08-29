package controllers

import javax.inject.Inject

import controllers.security.AuthenticatedAction
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller
import services.RepositoryService

import scala.concurrent.ExecutionContext

/**
 * Created by Sergey Tarkhanov on 7/31/2015.
 */
class SessionController @Inject()(val messagesApi: MessagesApi, repositoryService: RepositoryService)(implicit ex: ExecutionContext) extends Controller with I18nSupport with RequestImplicits {

  private val defaultRedirect = Redirect(Global.defaultLandingPage)

  def commit = AuthenticatedAction async {
    implicit request =>
      repositoryService.commit(request.user).map {
        _ => defaultRedirect
      }.recover {
        case ex: IllegalStateException => defaultRedirect withError ex
        case ex: IllegalArgumentException => defaultRedirect withError ex
      }
  }

  def cancel = AuthenticatedAction async {
    implicit request =>
      repositoryService.cancel(request.user).map {
        _ => defaultRedirect
      }
  }

}
