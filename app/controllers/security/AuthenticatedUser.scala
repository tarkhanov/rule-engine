package controllers.security

trait AuthenticatedUser {
  def salt: String
  def password: String
  def login: String
  def uid: String
}
