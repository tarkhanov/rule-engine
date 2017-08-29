package controllers

import javax.inject.Inject

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._

class PublicController @Inject()(val messagesApi: MessagesApi) extends Controller with I18nSupport {

  def index = Action {
    implicit request =>
      val authenticatedSession = request.session.get("user")
      Ok(views.html.index("Your new application is ready.", authenticatedSession))
  }

}