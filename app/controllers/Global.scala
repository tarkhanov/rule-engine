package controllers

/**
 * Created by Sergey Tarkhanov on 1/28/2015.
 */

import java.io.{PrintWriter, StringWriter}

import play.api.i18n.Messages
import play.api.mvc.Results.InternalServerError
import play.api.mvc._
import play.api.{GlobalSettings, Play}

import scala.concurrent.Future

object Global extends GlobalSettings {

  val defaultLandingPage: Call = routes.RepositoryController.dashboard(None, None, None)
  val loginPage: Call = routes.AuthenticationController.login()
  val logoutPage: Call = routes.PublicController.index()

  import Messages.Implicits.applicationMessagesApi

  def internalServerError(ex: Throwable)(implicit request: RequestHeader): Result = {
    implicit val messages: Messages = applicationMessagesApi(Play.current).preferred(request)
    InternalServerError(views.html.error.http500(ex, Some(stackTraceToString(ex))))
  }

  override def onError(request: RequestHeader, ex: Throwable): Future[Result] =
    Future.successful(internalServerError(ex)(request))

  private def stackTraceToString(ex: Throwable): String = {
    val sw = new StringWriter()
    ex.printStackTrace(new PrintWriter(sw))
    sw.getBuffer.toString
  }

}