package controllers.security

import controllers.security.WebSecurity.Credentials

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait Authenticator[UserType] {
  def authenticate(credentials: Credentials)(implicit ec: ExecutionContext): Future[Try[UserType]]
}