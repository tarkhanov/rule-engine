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
  class AuthenticatedRequest[A](val user: String, request: Request[A]) extends WrappedRequest[A](request)
  class AuthenticatedRequestWS(val user: String, val request: RequestHeader)

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

  private def authenticatedUser(request: RequestHeader) = request.session.get(USER_SESSION_KEY)

  def authenticatedWS[R](block: AuthenticatedRequestWS => Future[R])
                        (request: RequestHeader)
                        (implicit ec: ExecutionContext): Future[Either[Results.Status, R]] = {
    val authenticated = authenticatedUser(request)
    if (authenticated.nonEmpty)
      block(new AuthenticatedRequestWS(authenticated.get, request)).map(Right(_))
    else
      Future.successful(Left(Results.Forbidden))
  }

  def authenticated[A](request: Request[A])
                      (block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    val authenticated = authenticatedUser(request)
    if (authenticated.nonEmpty)
      block(new AuthenticatedRequest(authenticated.get, request))
    else {
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
