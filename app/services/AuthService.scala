package services

import javax.inject.{Inject, Singleton}

import com.typesafe.scalalogging.StrictLogging
import models.users.User
import services.auth.AuthenticationService

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}


/*
@Singleton
class AuthService @Inject()(userService: UserService)(implicit ec: ExecutionContext) extends AuthenticationService[User](userService) with StrictLogging {

  def init(): Unit = {

    def defineUser(login: String, password: String): Unit = {

      def createUser(user: User): Unit = {
        userService.create(user).onComplete {
          case Success(_) => logger.info("User " + user.login + " was created")
          case Failure(ex) => logger.info("User not created: " + ex)
        }
      }

      userService.getUser(login).foreach {
        case None =>
          val userSalt = salt
          val encPassword = encodePassword(userSalt, password)
          val user = User(login, userSalt, encPassword)
          createUser(user)
        case Some(user) =>
          logger.info("User already exists: " + user)
      }
    }

    // Initial users
    defineUser("admin", "password")
    defineUser("user", "password")
  }

  init()

}
*/