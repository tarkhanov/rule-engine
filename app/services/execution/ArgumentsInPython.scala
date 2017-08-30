package services.execution

import models.repository.rules.RulesModel.Argument
import models.repository.types.TypesModel.{Type, TypeDefs}
import services.execution.RulesPythonExecutor.{RequestDataType, RequestDataValueType}
import services.types.TypeDefinitionService
import services.types.TypeDefinitionService.TypeCacheType

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object ArgumentsInPython {

  def generateArguments(arguments: List[Argument],
                        requestData: RequestDataType,
                        types: TypeDefinitionService,
                        typeCache: TypeCacheType)
                       (implicit ec: ExecutionContext): Future[String] = {

    val futures = arguments.map(arg => {
      val values = (argName: String) => requestData.getOrElse(argName, Seq.empty).take(1)
      generatePythonCodeForArgument(arg.name, arg.`type`, values, types, typeCache).map(arg.name -> _)
    })
    Future.sequence(futures).map(_.toMap.mapValues(_.headOption.orNull)).map {
      map =>
        objectView + "\n" + map.map { case (k,v) => s"$k = $v" }.mkString("\n")
    }
  }

  private val objectView =
    """class objectview(object):
      |    def __init__(self, d):
      |        self.__dict__ = d
    """.stripMargin

  private def generatePythonCodeForArgument(name: String,
                                            typeId: String,
                                            requestDataValues: String => RequestDataValueType,
                                            types: TypeDefinitionService,
                                            typeCache: TypeCacheType)
                                           (implicit ec: ExecutionContext): Future[Seq[String]] = {
    typeId match {
      case "int" =>
        val integerValue = Try(requestDataValues(name).flatMap(_.left.toOption.map(v => Integer.parseInt(v))))
        integerValue match {
          case Success(value) => Future.successful(value.map { i => i.toString })
          case Failure(ex) => Future.failed(new IllegalArgumentException("Unable to parse integer value", ex))
        }

      case "string" =>
        val stringValue = requestDataValues(name).flatMap(_.left.toOption)
        Future.successful(stringValue.map { s => s"'$s'" })

      case TypeDefs.isList(itemType) =>
        val list = requestDataValues(name + "s").map {
          i => {
            val is = i.right.map(_.asInstanceOf[RequestDataType]).right.toOption
            val getLevelItems = (name: String) => is.map(_.getOrElse(name, Seq.empty)).getOrElse(Seq.empty)
            generatePythonCodeForArgument(name, itemType, getLevelItems, types, typeCache)
          }
        }
        def convertSeqToPyList(spo: Seq[String]) = spo.mkString("[", ", ", "]")
        Future.sequence(list).map(_.map(convertSeqToPyList))

      case otherType =>
        types.typeDefinitionLookup(otherType, typeCache).flatMap {
          case Some((_, typeDef: Type)) =>
            val mapOfFields = requestDataValues(name).headOption.map(_.right.get.asInstanceOf[RequestDataType])
            val pyFieldsFutures = typeDef.fields.map(f => {
              def getLevelField(fieldName: String) = mapOfFields.map(_.getOrElse(fieldName, Seq.empty).take(1)).getOrElse(Seq.empty) // Get 1st argument of structure
              generatePythonCodeForArgument(f.name, f.typeDef, getLevelField, types, typeCache).map(f.name -> _.headOption.orNull)
            })
            Future.sequence(pyFieldsFutures).flatMap {
              pyFields =>
                val obj = "objectview({ " + pyFields.map { case (fieldName, value) => s"'$fieldName': $value" }.mkString(", ") + " })"
                Future.successful(Seq(obj))
            }
          case _ =>
            throw new IllegalStateException("Type not found: " + otherType)
        }
    }
  }


}
