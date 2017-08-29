package controllers

import java.io.{PrintWriter, StringWriter}
import javax.inject.{Inject, Singleton}

import play.api.http.HttpErrorHandler
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results._
import play.api.mvc.{RequestHeader, _}

import scala.concurrent._

@Singleton
class ErrorHandler @Inject()(val messagesApi: MessagesApi) extends HttpErrorHandler with I18nSupport {

  def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    Future.successful(
      Status(statusCode)("A client error occurred: " + message)
    )
  }

  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    implicit val requestHeader: RequestHeader = request
    Future.successful(
      InternalServerError(views.html.error.http500(exception, Some(stackTraceToString(exception))))
    )
  }

  private def stackTraceToString(ex: Throwable): String = {
    val sw = new StringWriter()
    ex.printStackTrace(new PrintWriter(sw))
    sw.getBuffer.toString
  }


}
