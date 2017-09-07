package models.users

import controllers.security.AuthenticatedUser

case class User(login: String, salt: String, password: String) extends AuthenticatedUser {
  def uid: String = login
}

