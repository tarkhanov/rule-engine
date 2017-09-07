package services.auth

import java.util.Calendar
import javax.inject.Inject

import controllers.security.{AuthenticatedUser, Authenticator}
import controllers.security.WebSecurity.Credentials
import org.apache.commons.codec.digest.DigestUtils
import services.auth.AuthenticationService.{AuthenticationException, UserNotFoundException}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object AuthenticationService {
  class AuthenticationException(message: String) extends RuntimeException(message)
  class UserNotFoundException(message: String) extends RuntimeException(message)
}

class AuthenticationService[UserType <: AuthenticatedUser] @Inject()(userService: AuthenticationUserService[UserType]) extends Authenticator {

  def salt: String = Calendar.getInstance().get(Calendar.MILLISECOND).toString

  def encodePassword(salt: String, password: String): String =
    DigestUtils.md5Hex(salt + password)

  override def authenticate(credentials: Credentials)(implicit ec: ExecutionContext): Future[Try[UserType]] =
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
