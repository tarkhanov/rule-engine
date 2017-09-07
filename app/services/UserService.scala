package services

import models.users.User
import services.auth.AuthenticationUserService

import scala.concurrent.Future

trait UserService extends AuthenticationUserService[User] {

  def create(user: User): Future[Unit]

  def list(offset: Int, length: Int): Future[Seq[User]]

  def count: Future[Int]

}
