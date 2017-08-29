package services.types

import java.io.InputStream

import com.typesafe.scalalogging.StrictLogging
import models.repository.types.TypeRepositoryRec
import services.configuration.ConfigurationFilter
import services.configuration.ConfigurationService.UploadStatus
import models.repository.types.TypeModelXML._

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try
import scala.xml.{Elem, XML}

/**
 * Created by Sergey Tarkhanov on 6/5/2015.
 */
trait ConfigureTypesService extends ConfigurationFilter with StrictLogging {

  val ending = "MODEL.XML"

  override def applyConfigurationFilter(name: String, stream: => InputStream, user: String)(implicit ex: ExecutionContext): Future[List[Try[UploadStatus]]] = {
    if (name.toUpperCase.endsWith(ending)) {
      val doc: Elem = util.using(stream)(XML.load)
      val deployFutures = readTypes(doc).map(t => {
        logger.debug("Deploy new TYPE, seq: " + t.seq + " name: " + t.name)
        create(t.seq, TypeRepositoryRec(0L, "", t.name, serialize(t)), user)
      })
      val resultFutures = deployFutures.map { future =>
        anywayMap[Long, Try[UploadStatus]](future, a => a.map(formatUploadStatus(name)))
      }
      Future.sequence(resultFutures)
    }
    else
      Future.successful(List.empty)
  }


  def create(newSeq: Option[String], newRec: TypeRepositoryRec, user: String): Future[Long]

  private def formatUploadStatus(name: String)(id: Long): UploadStatus =
    UploadStatus(id, name + " uploaded successfully")

  def anywayMap[A, R](future: Future[A], f: Try[A] => R)(implicit executor: ExecutionContext): Future[R] = {
    val p = Promise[R]()
    future.onComplete { v => p complete Try(f(v)) }
    p.future
  }

}
