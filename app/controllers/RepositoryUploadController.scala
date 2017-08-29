package controllers

import java.io.FileInputStream
import java.util.function.Consumer
import java.util.zip.{ZipEntry, ZipFile}
import javax.inject.Inject

import controllers.security.AuthenticatedAction
import controllers.security.WebSecurity.AuthenticatedRequest
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.Files.TemporaryFile
import play.api.mvc.Controller
import play.api.mvc.MultipartFormData.FilePart
import services.configuration.ConfigurationService
import services.configuration.ConfigurationService.UploadStatus

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
 * Created by Sergey Tarkhanov on 8/13/2015.
 */
class RepositoryUploadController @Inject()(val messagesApi: MessagesApi, configurationService: ConfigurationService)(implicit ec: ExecutionContext) extends Controller with I18nSupport with RequestImplicits {

  private val uploadControllerIndex = Redirect(routes.RepositoryUploadController.upload())

  def upload = AuthenticatedAction {
    implicit request =>
      Ok(views.html.repository.upload(request.user, request.error, request.log))
  }

  private def statusToString(status: Try[UploadStatus]): String =
    status match {
      case Success(UploadStatus(id, message)) => id + " - " + message
      case Failure(ex) => ex.getClass.getName + " - " + ex.getMessage
    }

  private def collectLog(list: Seq[List[Try[UploadStatus]]]): String =
    list.flatten.foldLeft(new StringBuilder()) { (sb, i) => sb.append(statusToString(i) + "\n") }.toString()

  private def readZipFile(uploadedFile: FilePart[TemporaryFile])(implicit request: AuthenticatedRequest[_]) = {
    val zip = new ZipFile(uploadedFile.ref.file, ZipFile.OPEN_READ)
    val futuresM = mutable.MutableList[Future[List[Try[UploadStatus]]]]()
    util.using(zip) {
      _.stream().forEach {
        new Consumer[ZipEntry]() {
          override def accept(f: ZipEntry): Unit =
            futuresM += configurationService.process(f.getName, zip.getInputStream(f), request.user)
        }
      }
    }
    futuresM.toList
  }

  def uploadFiles = AuthenticatedAction.async(parse.multipartFormData) {
    implicit request =>
      try {
        val futures = for {
          uploadedFile <- request.body.files
          uploadedItem <- if (uploadedFile.filename.toLowerCase.endsWith(".zip"))
            readZipFile(uploadedFile)
          else
            Seq(configurationService.process(uploadedFile.filename, new FileInputStream(uploadedFile.ref.file), request.user))
        } yield uploadedItem
        Future.sequence(futures).map { r => uploadControllerIndex flashing ("log" -> collectLog(r)) }
      }
      catch {
        case ex: Throwable => Future.successful(uploadControllerIndex withError ex)
      }
  }

}


