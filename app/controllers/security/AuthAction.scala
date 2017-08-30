package controllers.security

import javax.inject.Inject

import controllers.security.WebSecurity.AuthenticatedRequest
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class AuthAction @Inject()(val parser: BodyParsers.Default)
                          (implicit val executionContext: ExecutionContext)
                          extends ActionBuilder[AuthenticatedRequest, AnyContent] {

  def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]): Future[Result] = {
    WebSecurity.authenticated(request) {
      block(_)
    }
  }

}
