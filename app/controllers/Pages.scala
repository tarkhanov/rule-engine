package controllers

import play.api.mvc._

object Pages {

  val defaultLandingPage: Call = routes.RepositoryController.dashboard(None, None, None)
  val loginPage: Call = routes.AuthenticationController.login()
  val logoutPage: Call = routes.PublicController.index()

}