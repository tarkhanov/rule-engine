package controllers

import javax.inject.Inject

import controllers.security.AuthAction
import services.UserService

/**
 * Created by Sergey Tarkhanov on 7/31/2015.
 */
class AccountController @Inject()(authenticatedAction: AuthAction, userService: UserService) extends InternationalInjectedController {

  def details = authenticatedAction {
    implicit request =>
      Ok(views.html.account.account(request.user, request.log))
  }

}