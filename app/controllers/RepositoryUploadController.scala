package controllers

import java.io.FileInputStream
import java.util.zip.{ZipEntry, ZipFile}
import javax.inject.Inject

import controllers.security.AuthAction
import controllers.security.WebSecurity.AuthenticatedRequest
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import services.configuration.ConfigurationService
import services.configuration.ConfigurationService.UploadStatus

import scala.collection.mutable
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class RepositoryUploadController @Inject()(authenticatedAction: AuthAction, configurationService: ConfigurationService) extends InternationalInjectedController {

  private val uploadControllerIndex = Redirect(routes.RepositoryUploadController.upload())

  def upload = authenticatedAction {
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
    val zip = new ZipFile(uploadedFile.ref.path.toFile, ZipFile.OPEN_READ)
    val futuresM = mutable.MutableList[Future[List[Try[UploadStatus]]]]()
    utils.using(zip) {
      _.stream().forEach {
        (f: ZipEntry) => futuresM += configurationService.process(f.getName, zip.getInputStream(f), request.user)
      }
    }
    futuresM.toList
  }

  def uploadFiles = authenticatedAction.async(parse.multipartFormData) {
    implicit request =>
      try {
        val futures = for {
          uploadedFile <- request.body.files
          uploadedItem <- if (uploadedFile.filename.toLowerCase.endsWith(".zip"))
            readZipFile(uploadedFile)
          else
            Seq(configurationService.process(uploadedFile.filename, new FileInputStream(uploadedFile.ref.path.toFile), request.user))
        } yield uploadedItem
        Future.sequence(futures).map { r => uploadControllerIndex flashing ("log" -> collectLog(r)) }
      }
      catch {
        case ex: Throwable => Future.successful(uploadControllerIndex withError ex)
      }
  }

}


