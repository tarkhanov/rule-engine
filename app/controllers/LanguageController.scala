package controllers

import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Lang

class LanguageController extends InternationalInjectedController {

  def changeLanguage() = Action(parse.formUrlEncoded) {
    implicit request =>
      val referrer = request.headers.get(REFERER).getOrElse("/")
      val form = Form("language" -> nonEmptyText)
      form.bindFromRequest.fold(
        _ => BadRequest(referrer),
        language => Redirect(referrer).withLang(Lang(language))
      )
  }

}