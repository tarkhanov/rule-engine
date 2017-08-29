package services.configuration


import java.io.InputStream

import services.configuration.ConfigurationService.UploadStatus

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
 * Created by Sergey Tarkhanov on 4/3/2015.
 */
trait ConfigurationFilter {

  def applyConfigurationFilter(name: String, stream: => InputStream, user: String)(implicit ec: ExecutionContext): Future[List[Try[UploadStatus]]]

}
