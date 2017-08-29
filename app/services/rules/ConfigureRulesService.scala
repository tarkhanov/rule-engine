package services.rules

import java.io.InputStream

import models.repository.rules.{RuleRepositoryRec, RulesModelXML}
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.IOUtils
import services.configuration.ConfigurationFilter
import services.configuration.ConfigurationService.UploadStatus

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

/**
 * Created by Sergey Tarkhanov on 6/5/2015.
 */
trait ConfigureRulesService extends ConfigurationFilter {

  val ending = ".RULES.XML"

  override def applyConfigurationFilter(name: String, stream: => InputStream, user: String)(implicit ec: ExecutionContext): Future[List[Try[UploadStatus]]] = {

    if (name.toUpperCase.endsWith(ending)) {
      val zipInputStream = stream
      val doc = util.using(zipInputStream)(is => IOUtils.toString(is)).replaceAll("\\t", "    ")

      val ruleSet = RulesModelXML.parse(doc)
      val newSeq = ruleSet.seq
      val newName = name.substring(0, name.length - ending.length)
      val newApi = DigestUtils.md5Hex(ruleSet.arguments.toString.getBytes)

      val future = create(newSeq, RuleRepositoryRec(0L, "", newApi, newName, doc), user)
      anywayMap[Long, List[Try[UploadStatus]]](future, a => List(a.map(formatUploadStatus(newName))))
    }
    else
      Future.successful(List.empty)
  }

  def create(newSeq: Option[String], newRec: RuleRepositoryRec, user: String): Future[Long]

  private def formatUploadStatus(name: String)(id: Long): UploadStatus =
    UploadStatus(id, name + " uploaded successfully")

  def anywayMap[A, R](future: Future[A], f: Try[A] => R)(implicit executor: ExecutionContext): Future[R] = {
    val p = Promise[R]()
    future.onComplete { v => p complete Try(f(v)) }
    p.future
  }

}
