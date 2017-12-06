package controllers.security

import controllers.security.WebSecurity.Credentials

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait Authenticator {

  def validate(userId: String): Future[Option[AuthenticatedUser]]

  def authenticate(credentials: Credentials)(implicit ec: ExecutionContext): Future[Try[AuthenticatedUser]]
}