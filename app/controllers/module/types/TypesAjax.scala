package controllers.module.types

import javax.inject.Inject

import controllers.security.AuthenticatedAction
import models.repository.types.{TypeModelXML, TypesModel}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json._
import play.api.mvc.Controller
import models.repository.types.TypesModel.{Field, Type}
import services.types.TypesService

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

/**
 * Created by Sergey Tarkhanov on 7/17/2015.
 */
class TypesAjax @Inject()(val messagesApi: MessagesApi, typesService: TypesService)(implicit ec: ExecutionContext) extends Controller with I18nSupport {

  implicit object writeListOfPairs extends Writes[List[(String, String)]] {
    def writes(list: List[(String, String)]) =
      JsArray(list.map { case (typeDef, name) => JsObject(Seq("type" -> JsString(typeDef), "name" -> JsString(name))) })
  }

  def getListOfTypes(listName: String): List[(String, String)] =
    TypesModel.builtInTypes.map(item => item -> item)

  def available(listName: String) = AuthenticatedAction async {
    implicit request =>
      val listOfTypes = getListOfTypes(listName)
      Future.successful(Ok(Json.toJson(listOfTypes)).as("application/json"))
  }

  def save(id: Long) = AuthenticatedAction.async(parse.json) {
    request =>
      request.body.result.toOption match {
        case Some(jsonTypeDefinition) =>
          val typeDefinition = jsonDeserializeType(jsonTypeDefinition)
          val xmlTypeDefinition = TypeModelXML.serialize(typeDefinition)
          val sequence = typeDefinition.seq
          val name = Some(typeDefinition.name)
          typesService.saveDefinition(id, xmlTypeDefinition, sequence, name).map {
            _ =>
              val response = JsObject(Seq("status" -> JsString("OK")))
              Ok(response).as("application/json")
          }.recover { case ex =>
              val response = JsObject(Seq("status" -> JsString("ERROR"), "error" -> JsString(ex.getMessage)))
              Ok(response).as("application/json")
          }
        case None =>
          Future.successful(BadRequest)
      }
  }

  def jsonDeserializeType(json: JsValue): Type = {

    implicit def string(v: JsValue): String = v match {
      case v: JsString => v.value
      case _ => throw new IllegalArgumentException
    }

    def field(v: JsValue): Field = v match {
      case v: JsObject => Field(v.value("name"), v.value("type"))
      case _ => throw new IllegalArgumentException
    }

    json match {
      case o: JsObject =>
        val seq = o.value.get("seq").map(string)
        val name = o.value("name")
        val fields = o.value("fields") match {
          case fields: JsArray => fields.value.map(field)
          case _ => throw new IllegalArgumentException
        }
        Type(seq, name, fields.toList)
      case _ => throw new IllegalArgumentException
    }
  }

}
