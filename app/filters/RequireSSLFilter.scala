package filters

import play.api.mvc.{Filter, RequestHeader, Result, Results}

import scala.concurrent.Future

/**
 * Created by Sergey Tarkhanov on 1/28/2015.
 */

class RequireSSLFilter(securedUris: Seq[String]) extends Filter {

  def apply(nextFilter: (RequestHeader) => Future[Result])(requestHeader: RequestHeader): Future[Result] =
    if (!requestHeader.secure) {
      val securedUri = uriMustBeSecure(requestHeader)
      if (securedUri.nonEmpty)
        Future.successful(Results.TemporaryRedirect("https://" + requestHeader.host + requestHeader.uri))
      else
        nextFilter(requestHeader)
    }
    else
      nextFilter(requestHeader)

  def uriMustBeSecure(implicit requestHeader: RequestHeader): Option[String] =
    securedUris.find(securedUri => requestHeader.uri.startsWith(securedUri))

}

object RequireSSLFilter {

  def apply(securedUris: String*) = new RequireSSLFilter(securedUris.toSeq)

}