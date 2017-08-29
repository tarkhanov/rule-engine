package services.auth

import scala.concurrent.Future

/**
 * Created by Sergey Tarkhanov on 5/24/2015.
 */
trait AuthenticationUserService[User <: AuthenticationUser] {

  def getUser(uid: String): Future[Option[User]]

}
