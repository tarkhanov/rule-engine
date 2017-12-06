package controllers.security

import controllers.Pages
import play.api.http.HttpVerbs
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object WebSecurity {

  case class Credentials(login: String, password: String)

  type RequestBody = Map[String, Seq[String]]

  class TryAuthenticatedRequest[A, U](val user: Try[U], request: Request[A]) extends WrappedRequest[A](request)
  class AuthenticatedRequest[A](val user: AuthenticatedUser, request: Request[A]) extends WrappedRequest[A](request)
  class AuthenticatedRequestWS(val user: AuthenticatedUser, val request: RequestHeader)

  private val USER_SESSION_KEY = "user"

  def login(request: Request[RequestBody], credentials: Credentials, authenticator: Authenticator)
                                   (block: TryAuthenticatedRequest[RequestBody, AuthenticatedUser] => Result)
                                   (implicit ec: ExecutionContext): Future[Result] = {

    authenticator.authenticate(credentials).map {
      case Success(user) =>
        val result = block(new TryAuthenticatedRequest(Success(user), request))
        result.addingToSession(USER_SESSION_KEY -> user.uid)(request)
      case Failure(e) =>
        block(new TryAuthenticatedRequest(Failure(e), request))
    }
  }

  def turnOut[A](option: Option[Future[A]])(implicit ec: ExecutionContext): Future[Option[A]] =
    option.map(_.map(Some.apply)).getOrElse(Future.successful(None))

  private def authenticatedUser(request: RequestHeader) = request.session.get(USER_SESSION_KEY)

  def authenticatedWS[R](authenticator: Authenticator)
                        (block: AuthenticatedRequestWS => Future[R])
                        (request: RequestHeader)
                        (implicit ec: ExecutionContext): Future[Either[Results.Status, R]] = {

    turnOut(authenticatedUser(request).map(authenticator.validate)).map(_.flatten).flatMap {
      case Some(authenticated) =>
        block(new AuthenticatedRequestWS(authenticated, request)).map(Right(_))
      case None =>
        Future.successful(Left(Results.Forbidden))
    }
  }

  def authenticated[A](request: Request[A], authenticator: Authenticator)
                      (block: AuthenticatedRequest[A] => Future[Result])
                      (implicit ec: ExecutionContext): Future[Result] = {

    turnOut(authenticatedUser(request).map(authenticator.validate)).map(_.flatten).flatMap {
      case Some(authenticated) =>
        block(new AuthenticatedRequest(authenticated, request))
      case None =>
        val redirect = Results.TemporaryRedirect(Pages.loginPage.url)
        val result = if (request.method == HttpVerbs.GET)
          redirect.flashing("redirect" -> request.uri)
        else
          redirect
        Future.successful(result)
    }
  }

  def logout[A](request: Request[A])(block: Request[A] => Result): Future[Result] = {
    Future.successful(block(request).withNewSession)
  }

}
