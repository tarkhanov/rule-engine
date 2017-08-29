package services.auth

/**
 * Created by Sergey Tarkhanov on 5/24/2015.
 */
trait AuthenticationUser {
  def salt: String
  def password: String
  def login: String
  def uid: String
}
