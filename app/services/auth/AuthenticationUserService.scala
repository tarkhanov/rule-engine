package services.auth

import controllers.security.AuthenticatedUser

import scala.concurrent.Future

trait AuthenticationUserService[User <: AuthenticatedUser] {

  def getUser(uid: String): Future[Option[User]]

}
