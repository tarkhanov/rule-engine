package controllers

import javax.inject.Inject

import controllers.AuthenticationController.LoginFormData
import controllers.security.WebSecurity.Credentials
import controllers.security.{AuthenticatedAction, WebSecurity}
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services.AuthService
import util._

import scala.concurrent.Future
import scala.util.{Failure, Success}

object AuthenticationController {

  case class LoginFormData(uid: String, password: String, redirect: Option[String])

}

class AuthenticationController @Inject()(val messagesApi: MessagesApi, authService: AuthService) extends Controller with I18nSupport with RequestImplicits {

  private val defaultLandingPage = Global.defaultLandingPage
  private val loginPage = Global.loginPage
  private val logoutPage = Global.logoutPage

  def login = Action {
    drawLoginForm(_, None)
  }

  private val userLoginFrom = Form(mapping(
    "login" -> text.verifying("Login is empty", _.nonEmpty),
    "password" -> text.verifying("Password is empty", _.nonEmpty),
    "redirect" -> optional(text)
  )(LoginFormData.apply)(LoginFormData.unapply))

  def enter = Action.async(parse.urlFormEncoded) {
    implicit request =>
      userLoginFrom.bindFromRequest.continue {
        case Success(data) =>
          val credentials = Credentials(data.uid, data.password)
          WebSecurity.login(request, credentials, authService) {
            _.user match {
              case Success(_) =>
                SeeOther(data.redirect.flatMap(nonEmptyOption).getOrElse(defaultLandingPage.url))
              case Failure(e) =>
                Redirect(loginPage) withError e
            }
          }
        case Failure(error) =>
          Future.successful(Redirect(loginPage) withError error)
      }
  }

  def logout = AuthenticatedAction async {
    WebSecurity.logout(_) {
      _ => Redirect(logoutPage)
    }
  }

  private def drawLoginForm[A](request: Request[A], user: Option[String]): Result = {
    Ok(views.html.login("Your new application is ready.", user, request.error, request.redirect))
  }

  private def nonEmptyOption(string: String): Option[String] =
    if (string.nonEmpty)
      Some(string)
    else
      None
}