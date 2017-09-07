package controllers.module.types

import java.io.StringReader
import javax.inject.Inject

import controllers.InternationalInjectedController
import controllers.security.AuthAction
import models.repository.types.TypesModel._
import models.repository.types.{TypeModelXML, TypeRepositoryRec}
import persistence.repository.Repository
import services.types.TypesService

import scala.concurrent.{ExecutionContext, Future}

class TypesController @Inject()(authenticatedAction: AuthAction, typesService: TypesService)(implicit ec: ExecutionContext) extends InternationalInjectedController {

  def create = authenticatedAction async {
    implicit request =>

      import models.repository.types.TypeModelXML._

      val newTypeDefinition = Type(None, "New Type", List(Field("field1", "string")))
      val rec = TypeRepositoryRec(0L, "", newTypeDefinition.name, serialize(newTypeDefinition))

      for {
        created <- typesService.create(Some("new"), rec, request.user)
        result = Redirect(routes.TypesController.open(created))
      } yield result
  }

  def open(id: Long) = authenticatedAction async {
    implicit request =>
      typesService.getRecordDetails(id, request.user).map {
        case Some(rec) =>
          val readOnly = !rec.actions.exists(_.action == Repository.CREATE_ACTION)
          val fields = TypeModelXML.readFields(scala.xml.XML.load(new StringReader(rec.record.definition)))
          Ok(views.html.module.types.open(request.user, readOnly, rec, fields, request.flash.get("log")))
        case None =>
          NotFound(views.html.error.http404(request.user))
      }
  }

  def edit(id: Long) = authenticatedAction async {
    implicit request =>
      typesService.lookupId(id).flatMap {
        case Some(current) => typesService.create(Some(current.seq), current, request.user)
        case None => Future.failed(new IllegalStateException("Record not found"))
      }.map {
        rec => Redirect(routes.TypesController.open(rec))
      }.recover {
        case _: IllegalStateException => NotFound(views.html.error.http404(request.user))
      }
  }
}
