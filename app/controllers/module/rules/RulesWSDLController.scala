package controllers.module.rules

import javax.inject.Inject

import controllers.module.rules.RulesWSDLController.TypeNotFoundException
import controllers.{InternationalInjectedController, Pages}
import models.repository.rules.RulesModelXML
import models.repository.types.TypeRepositoryRec
import models.repository.types.TypesModel._
import play.api.mvc._
import services.execution.RulesWSDLDoc._
import services.execution.RulesWSDLDoc
import services.rules.RulesService
import services.types.TypeDefinitionService
import services.types.TypeDefinitionService.TypeCacheType

import scala.collection.mutable
import scala.concurrent.Future

object RulesWSDLController {
  class TypeNotFoundException(message: String) extends RuntimeException(message)
}

class RulesWSDLController @Inject()(rulesService: RulesService, typeDefinitionService: TypeDefinitionService) extends InternationalInjectedController {

  private val defaultRedirect: Result = Redirect(Pages.defaultLandingPage)

  private def createType(name: String, typeId: String, list: mutable.Map[String, Option[SchemaType]], typeCache: TypeCacheType): Future[SchemaElement] =
    typeId match {
      case TypeDefs.isBasicType(_) => Future.successful(SchemaElement(name, Left(typeId), Some(1), Some(1)))
      case TypeDefs.isList(itemTypeId) =>
        val el = createType(name, itemTypeId, list, typeCache).map(e => e.copy(minOccurs = Some(0), maxOccurs = Some(Int.MaxValue)))
        el.map(e => SchemaElement(name + "s", Right(SchemaType(None, List(e), List())), Some(1), Some(1)))

      case _ =>
        typeDefinitionService.typeDefinitionLookup(typeId, typeCache).flatMap {
          case Some(typeDef) =>
            list.put(typeDef.name, None)
            val elements = typeDef.fields.map {
              field =>
                if (!list.contains(field.typeDef))
                  createType(field.name, field.typeDef, list, typeCache)
                else
                  Future.successful(SchemaElement(field.name, Left(field.typeDef)))
            }
            Future.sequence(elements).map {
              elems =>
                list.put(typeDef.name, Some(SchemaType(Some(typeDef.name), elems)))
                SchemaElement(name, Left(typeDef.name), Some(1), Some(1))
            }
          case _ =>
            throw new TypeNotFoundException("Type " + typeId + " was not found")
        }
    }

  def wsdl(ruleSetFilter: String, wsdl: String): Action[AnyContent] = Action.async {
    _ =>
      rulesService.lookup(ruleSetFilter).flatMap {
        case Some(set) =>

          val xml = RulesModelXML.parse(set.definition)
          val mapOfTypes = mutable.Map[String, Option[SchemaType]]()
          val typeCache: TypeCacheType = typeDefinitionService.newTypeCache

          val requestElementsFutures = xml.arguments.list.map(a => createType(a.name, a.`type`, mapOfTypes, typeCache))
          val responseElementsFutures = xml.results.list.map(r => createType(r.name, r.`type`, mapOfTypes, typeCache))

          Future.sequence(requestElementsFutures).flatMap {
            requestElements =>
              Future.sequence(responseElementsFutures).map {
                responseElements =>

                  val rulesRequestType = SchemaType(None, requestElements)
                  val rulesRequest = SchemaElement("rulesRequest", Right(rulesRequestType))

                  val responseAttributes = List(SchemaAttribute("name", Left("string"), required = false), SchemaAttribute("condition", Left("boolean"), required = false))
                  val rule = SchemaElement("rule", Right(SchemaType(None, responseElements, responseAttributes)), Some(0), Some(Int.MaxValue))
                  val rulesResponseType = SchemaType(None, List(rule))

                  val rulesResponse = SchemaElement("rulesResponse", Right(rulesResponseType))
                  val elements = List(rulesRequest, rulesResponse)

                  val documentation = "Service for rule set: " + set.name + " (" + ruleSetFilter + ") => " + set.id
                  val c = RulesWSDLDoc.create(ruleSetFilter, elements, mapOfTypes.filter(_._2.isDefined).map(_._2.get).toList, documentation)

                  Ok(c).as("text/xml")
              }
          }
        case None =>
          Future.successful(NotFound)
      }.recover {
        case ex: TypeNotFoundException => defaultRedirect withError ex
      }
  }
}
