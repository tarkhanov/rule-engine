package controllers

import javax.inject.Inject

import controllers.security.AuthAction
import services.UserService

class AccountController @Inject()(authenticatedAction: AuthAction, userService: UserService) extends InternationalInjectedController {

  def details = authenticatedAction {
    implicit request =>
      Ok(views.html.account.account(request.user.uid, request.log))
  }

}