package services

import javax.inject.{Inject, Singleton}

import models.users.User
import persistence.users.UserRepository
import services.auth.AuthenticationUserService

import scala.concurrent.Future

@Singleton
class UserService @Inject()(userRepository: UserRepository) extends AuthenticationUserService[User] {

  override def getUser(uid: String): Future[Option[User]] = userRepository.getUser(uid)

  def create(user: User): Future[Unit] = userRepository.create(user)

  def list(offset: Int, length: Int): Future[Seq[User]] = userRepository.list(offset, length)

  def count: Future[Int] = userRepository.count

}
