package controllers

import javax.inject.Inject

import controllers.security.AuthenticatedAction
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller
import services.UserService

/**
 * Created by Sergey Tarkhanov on 7/31/2015.
 */
class AccountController @Inject()(val messagesApi: MessagesApi, userService: UserService) extends Controller with I18nSupport with RequestImplicits {

  def details = AuthenticatedAction {
    implicit request =>
      Ok(views.html.account.account(request.user, request.log))
  }

}