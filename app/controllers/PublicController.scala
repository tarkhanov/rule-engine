package controllers

class PublicController extends InternationalInjectedController {

  def index = Action {
    implicit request =>
      val authenticatedSession = request.session.get("user")
      Ok(views.html.index("Your new application is ready.", authenticatedSession))
  }

}