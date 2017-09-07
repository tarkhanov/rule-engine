package persistence.users

import models.users.User

import scala.concurrent.Future

trait UserRepository {

  def getUser(login: String): Future[Option[User]]

  def create(user: User): Future[Unit]

  def list(offset: Int, length: Int): Future[Seq[User]]

  def count: Future[Int]

}
