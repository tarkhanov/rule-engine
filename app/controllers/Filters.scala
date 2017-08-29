package controllers

/**
 * Created by Sergey Tarkhanov on 10/29/2015.
 */

import javax.inject.Inject

import com.mohiva.play.htmlcompressor.HTMLCompressorFilter
import filters.RequireSSLFilter
import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter

class Filters @Inject()(htmlCompressorFilter: HTMLCompressorFilter) extends HttpFilters {

  val redirectFilter = RequireSSLFilter("/private", "/login", "/logout")

  override def filters: Seq[EssentialFilter] = Seq(redirectFilter, htmlCompressorFilter)
}