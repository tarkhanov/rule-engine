package models.users

import services.auth.AuthenticationUser

case class User(login: String, salt: String, password: String) extends AuthenticationUser {
  def uid: String = login
}

