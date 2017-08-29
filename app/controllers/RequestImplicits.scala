package controllers

import play.api.mvc.{Request, Result}

/**
 * Created by Sergey Tarkhanov on 7/31/2015.
 */
trait RequestImplicits {

  implicit class RichRequest(request: Request[_]) {
    def redirect: Option[String] = request.flash.get("redirect")
    def log: Option[String] = request.flash.get("log")
    def error: Option[String] = request.flash.get("error")
  }

  implicit class ResultWithError(result: Result) {
    def withError(ex: Throwable): Result = result.flashing("error" -> ex.getMessage)
  }

  def sessionLong(prop: String)(implicit request: Request[_]) = request.session.get(prop).map(_.toLong)

  def sessionInt(prop: String)(implicit request: Request[_]) = request.session.get(prop).map(_.toInt)

  def sessionBoolean(prop: String)(implicit request: Request[_]) = request.session.get(prop).map(_.toBoolean)

  implicit class ResultWithSession(result: Result) {

    def sessionSet[V <: AnyVal](prop: String, value: V)(implicit request: Request[_]) =
      result.withSession(request.session + (prop -> value.toString))

    def sessionSet[V <: AnyVal](condition: Boolean, prop: String, value: V)(implicit request: Request[_]) =
      if (condition)
        result.withSession(request.session + (prop -> value.toString))
      else
        result
  }

}
