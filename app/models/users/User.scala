package models.users

import services.auth.AuthenticationUser

/**
 * Created by Sergey Tarkhanov on 5/19/2015.
 */

case class User(login: String, salt: String, password: String) extends AuthenticationUser {
  def uid: String = login
}

