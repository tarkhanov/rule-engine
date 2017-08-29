package controllers

import javax.inject.Inject

import controllers.security.AuthenticatedAction
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller
import services.RepositoryService

import scala.concurrent.ExecutionContext

/**
 * Created by Sergey Tarkhanov on 4/5/2015.
 */
class RepositoryController @Inject()(val messagesApi: MessagesApi, repositoryService: RepositoryService)(implicit ec: ExecutionContext) extends Controller with I18nSupport with RequestImplicits {

  private val defaultRedirect = Redirect(Global.defaultLandingPage)

  def dashboard(folder: Option[Long], pageStart: Option[Int], pageSize: Option[Int], allRecords: Option[Boolean]) = AuthenticatedAction async {
    implicit request =>

      val recordFolder = folder.orElse(sessionLong("repoFolder"))
      val recordPageStart = pageStart.orElse(sessionInt("repoPageStart")).map(_.max(1)).getOrElse(1) - 1
      val recordPageSize = pageSize.orElse(sessionInt("repoPageSize")).map(_.max(1).min(500)).getOrElse(20)
      val recordAllFilter = allRecords.orElse(sessionBoolean("repoAllFilter")).getOrElse(false)

      val homeFolderAsNone = recordFolder.flatMap(f => if (f < 0) None else recordFolder)

      for {
        items <- repositoryService.list(homeFolderAsNone, request.user, recordPageStart, recordPageSize)
        result = Ok(views.html.repository.dashboard(request.user, items, recordPageStart + 1, recordPageSize, recordAllFilter, request.error, request.log))
          .sessionSet(recordFolder.isDefined, "repoFolder", recordFolder.getOrElse(0))
          .sessionSet(pageStart.isDefined, "repoPageStart", recordPageStart + 1)
          .sessionSet(pageSize.isDefined, "repoPageSize", recordPageSize)
          .sessionSet(allRecords.isDefined, "repoAllFilter", recordAllFilter)
      } yield result
  }

  def remove(recordId: Long) = AuthenticatedAction async {
    implicit request =>
      repositoryService.remove(Set(recordId), request.user).map {
        _ => defaultRedirect
      }.recover {
        case ex => defaultRedirect withError ex
      }
  }

  def sequence(recordId: Long, pageStart: Option[Int], pageSize: Option[Int]) = AuthenticatedAction async {
    implicit request =>

      val historyPageStart = pageStart.orElse(sessionInt("historyPageStart")).map(_.max(1)).getOrElse(1) - 1
      val historyPageSize = pageSize.orElse(sessionInt("historyPageSize")).map(_.max(1).min(500)).getOrElse(20)
      repositoryService.details(recordId, historyPageStart, historyPageSize, request.user).map {
        details =>
          Ok(views.html.repository.sequence(request.user, details, historyPageStart + 1, historyPageSize))
            .sessionSet(pageStart.isDefined, "repoPageStart", historyPageStart + 1)
            .sessionSet(pageSize.isDefined, "repoPageSize", historyPageSize)
      }.recover {
        case ex => defaultRedirect withError ex
      }
  }

}


