package services.auth

import java.util.Calendar

import controllers.security.Authenticator
import controllers.security.WebSecurity.Credentials
import org.apache.commons.codec.digest.DigestUtils
import services.auth.AuthenticationService.{AuthenticationException, UserNotFoundException}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
 * Created by Sergey Tarkhanov on 1/28/2015.
 */

object AuthenticationService {
  class AuthenticationException(message: String) extends RuntimeException(message)
  class UserNotFoundException(message: String) extends RuntimeException(message)
}

class AuthenticationService[UserType <: AuthenticationUser](userService: AuthenticationUserService[UserType]) extends Authenticator[UserType] {

  def salt = Calendar.getInstance().get(Calendar.MILLISECOND).toString

  def encodePassword(salt: String, password: String): String =
    DigestUtils.md5Hex(salt + password)

  override def authenticate(credentials: Credentials): Future[Try[UserType]] =
    userService.getUser(credentials.login).map {
      case Some(user) =>
        val encodedCredentialsPassword = encodePassword(user.salt, credentials.password)
        if (user.password == encodedCredentialsPassword)
          Success(user)
        else
          Failure(new AuthenticationException("Wrong login or password"))
      case None =>
        Failure(new UserNotFoundException("User not found"))
    }
}
