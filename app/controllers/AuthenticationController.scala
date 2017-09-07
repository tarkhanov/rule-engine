package controllers

import javax.inject.Inject

import controllers.AuthenticationController.LoginFormData
import controllers.security.WebSecurity.Credentials
import controllers.security.{AuthAction, Authenticator, WebSecurity}
import play.api.data.Forms._
import play.api.data._
import utils._

import scala.concurrent.Future
import scala.util.{Failure, Success}

object AuthenticationController {

  case class LoginFormData(uid: String, password: String, redirect: Option[String])

}

class AuthenticationController @Inject()(authenticatedAction: AuthAction, authenticator: Authenticator) extends InternationalInjectedController {

  def login = Action {
      implicit request =>
        Ok(views.html.login("Your new application is ready.", None, request.error, request.redirect))
    }


  private val userLoginFrom = Form(mapping(
    "login" -> text.verifying("Login is empty", _.nonEmpty),
    "password" -> text.verifying("Password is empty", _.nonEmpty),
    "redirect" -> optional(text)
  )(LoginFormData.apply)(LoginFormData.unapply))

  def enter = Action.async(parse.formUrlEncoded) {
    implicit request =>
      userLoginFrom.bindFromRequest.continue {
        case Success(data) =>
          val credentials = Credentials(data.uid, data.password)
          WebSecurity.login(request, credentials, authenticator) {
            _.user match {
              case Success(_) =>
                SeeOther(data.redirect.flatMap(nonEmptyOption).getOrElse(Pages.defaultLandingPage.url))
              case Failure(e) =>
                Redirect(Pages.loginPage) withError e
            }
          }
        case Failure(error) =>
          Future.successful(Redirect(Pages.loginPage) withError error)
      }
  }

  def logout = authenticatedAction async {
    WebSecurity.logout(_) {
      _ => Redirect(Pages.logoutPage)
    }
  }

  private def nonEmptyOption(string: String): Option[String] =
    if (string.nonEmpty)
      Some(string)
    else
      None
}