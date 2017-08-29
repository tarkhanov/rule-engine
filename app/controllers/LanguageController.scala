package controllers

import javax.inject.Inject

import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.mvc._

import scala.concurrent.ExecutionContext

class LanguageController @Inject()(val messagesApi: MessagesApi)(implicit ec: ExecutionContext) extends Controller with I18nSupport {

  def changeLanguage() = Action(parse.urlFormEncoded) {
    implicit request =>
      val referrer = request.headers.get(REFERER).getOrElse("/")
      val form = Form("language" -> nonEmptyText)
      form.bindFromRequest.fold(
        _ => BadRequest(referrer),
        language => Redirect(referrer).withLang(Lang(language))
      )
  }
}