package controllers.security

import controllers.security.WebSecurity.AuthenticatedRequest
import play.api.mvc._

import scala.concurrent.Future

/**
 * Created by Sergey Tarkhanov on 1/29/2015.
 */

object AuthenticatedAction extends ActionBuilder[AuthenticatedRequest] {

  def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]): Future[Result] = {
    WebSecurity.authenticated(request) {
      block(_)
    }
  }

}
