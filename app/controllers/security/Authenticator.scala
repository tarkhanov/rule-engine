package controllers.security

import controllers.security.WebSecurity.Credentials

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
 * Created by Sergey Tarkhanov on 5/24/2015.
 */
trait Authenticator[UserType] {
  def authenticate(credentials: Credentials)(implicit ec: ExecutionContext): Future[Try[UserType]]
}