package controllers

import javax.inject.Inject

import controllers.security.AuthAction
import services.RepositoryService

import scala.concurrent.ExecutionContext

class RepositoryController @Inject()(authenticatedAction: AuthAction, repositoryService: RepositoryService)(implicit ec: ExecutionContext) extends InternationalInjectedController {

  private val defaultRedirect = Redirect(Pages.defaultLandingPage)

  def dashboard(folder: Option[Long], pageStart: Option[Int], pageSize: Option[Int], allRecords: Option[Boolean]) = authenticatedAction async {
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

  def remove(recordId: Long) = authenticatedAction async {
    implicit request =>
      repositoryService.remove(Set(recordId), request.user).map {
        _ => defaultRedirect
      }.recover {
        case ex => defaultRedirect withError ex
      }
  }

  def sequence(recordId: Long, pageStart: Option[Int], pageSize: Option[Int]) = authenticatedAction async {
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


