package controllers

import javax.inject.Inject

import controllers.SettingsController.SettingsUserList
import controllers.security.AuthAction
import models.users.User
import services.UserService

/**
 * Created by Sergey Tarkhanov on 7/29/2015.
 */

object SettingsController {

  case class SettingsUserList(users: Seq[User], count: Int, pageStart: Int, pageSize: Int)

}

class SettingsController @Inject()(authenticatedAction: AuthAction, userService: UserService) extends InternationalInjectedController {

  def settings(pageStart: Option[Int], pageSize: Option[Int]) = authenticatedAction async {
    implicit request =>

      val usersPageStart = pageStart.orElse(sessionInt("usersPageStart")).map(_.max(1)).getOrElse(1) - 1
      val usersPageSize = pageSize.orElse(sessionInt("usersPageSize")).map(_.max(1).min(500)).getOrElse(20)

      val usersFuture = userService.list(usersPageStart, usersPageSize)
      val userCountFuture = userService.count

      for {
        users <- usersFuture
        countOfUsers <- userCountFuture
        userList = SettingsUserList(users, countOfUsers, usersPageStart + 1, usersPageSize)
        result = Ok(views.html.settings.users(request.user, userList, request.log))
          .sessionSet(pageStart.isDefined, "usersPageStart", usersPageStart + 1)
          .sessionSet(pageSize.isDefined, "usersPageSize", usersPageSize)
      } yield result
  }

}


