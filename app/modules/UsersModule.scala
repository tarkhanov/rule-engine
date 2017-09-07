package modules

import javax.inject.Inject

import com.google.inject.AbstractModule
import com.typesafe.scalalogging.StrictLogging
import controllers.security.Authenticator
import models.users.User
import modules.UsersModule.{InitializeUsers, Initializer, UserAuthenticationService}
import persistence.users.{UserRepository, UserRepositoryImpl}
import services.auth.AuthenticationService
import services.{UserService, UserServiceImpl}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object UsersModule {

  // TODO: Move user creation to startup handler

  trait Initializer {

  }

  class InitializeUsers @Inject()(userService: UserService)
                                 (implicit ec: ExecutionContext)
    extends AuthenticationService[User](userService) with Initializer with StrictLogging {

    private def defineUser(login: String, password: String): Unit = {

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


  class UserAuthenticationService @Inject()(userService: UserService) extends AuthenticationService[User](userService)

}

class UsersModule extends AbstractModule {

  override def configure(): Unit = {

    bind(classOf[UserRepository]).to(classOf[UserRepositoryImpl])
    bind(classOf[UserService]).to(classOf[UserServiceImpl])
    bind(classOf[Authenticator]).to(classOf[UserAuthenticationService])
    bind(classOf[Initializer]).to(classOf[InitializeUsers]).asEagerSingleton()

  }
}
