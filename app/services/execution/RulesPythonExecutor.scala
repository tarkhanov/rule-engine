package services.execution

import javax.inject.Inject

import com.typesafe.scalalogging.StrictLogging
import models.repository.rules.RulesModel.{Result, Rule}
import models.repository.rules.RulesModelXML
import models.repository.types.TypesModel.TypeDefs
import org.python.core._
import org.python.util.PythonInterpreter
import services.execution.RulesPythonExecutor._
import services.types.TypeDefinitionService
import services.types.TypeDefinitionService.TypeCacheType

import scala.collection.JavaConverters
import scala.concurrent.{ExecutionContext, Future}

object RulesPythonExecutor {

  type RequestDataValueType = Seq[Either[String, AnyRef]] // AnyRef instead of RequestDataType since IntelliJ had problems with recursive types
  type RequestDataType = Map[String, RequestDataValueType]

  case class ExecutionResult[T](value: T, exceptions: List[Throwable] = List.empty)

  type ConditionResult = ExecutionResult[Boolean]
  type BodyResult = ExecutionResult[List[(String, RuleResultType)]]

  type RuleResultType = List[Either[String, Map[String, AnyRef]]]

  class RuleExecutionException(message: String, ex: Throwable) extends Exception(message, ex)

}

class RulesPythonExecutor @Inject()(typeDefinitionService: TypeDefinitionService)(implicit ec: ExecutionContext) extends StrictLogging {

  def execute(requestData: RequestDataType, id: Long, definition: String): Future[List[(Rule, ConditionResult, Option[BodyResult])]] = {

    if (logger.underlying.isDebugEnabled) {
      logger.debug("Execute RuleSet with id = {}", id)
      for ((key, value) <- requestData) {
        logger.debug("Argument {} = {}", key, value.mkString(", "))
      }
    }

    try {
      val rulesDef = RulesModelXML.parse(definition)
      val arguments = rulesDef.arguments.list
      val types = typeDefinitionService.newTypeCache
      ArgumentsInPython.generateArguments(arguments, requestData, typeDefinitionService, types).flatMap { argumentsAsCode =>
        logger.debug("Arguments converted into python code: \n" + argumentsAsCode)
        val futures = rulesDef.rules.list.map(rule => executeRule(rule, argumentsAsCode, rulesDef.results.list, types))
        Future.sequence(futures)
      }
    }
    catch {
      case ex: Throwable =>
        Future.failed(ex)
    }
  }

  private def executeRule(rule: Rule, arguments: String, results: List[Result], types: TypeCacheType): Future[(Rule, ConditionResult, Option[BodyResult])] = {

    logger.debug("Execute Rule '{}'", rule.name.getOrElse("[Untitled]"))

    utils.using(new PythonInterpreter()) { pi =>

      val conditionResult = try {
        pi.exec(arguments)
        val result = pi.eval(rule.condition.code.trim)
        logger.debug("Invocation result is {}", result)
        ExecutionResult(result.equals(new PyBoolean(true)))
      }
      catch {
        case ex: PyException =>
          logger.debug("Exception during rule invocation: {}, {}", ex.getClass.getName, ex.getMessage)
          ExecutionResult(false, List(ex))
      }

      val bodyResult: Future[Option[BodyResult]] = if (conditionResult.value) {
        try {
          pi.exec(rule.body.code.trim)
          val futures = results.map(r => convertResponse(pi.get(r.name), r.`type`, types).map(r.name -> _))
          Future.sequence(futures).map(list => Some(ExecutionResult(list)))
        }
        catch {
          case ex: PyException =>
            Future.successful(Some(ExecutionResult(List(), List(ex))))
        }
      }
      else
        Future.successful(None)

      bodyResult.map(br => (rule, conditionResult, br))
    }
  }

  private def convertAnyTypeResponse(value: PyObject): Future[RuleResultType] = {
    value match {
      case integer: PyInteger => Future.successful(List(Left(integer.toString)))
      case string: PyString => Future.successful(List(Left(string.toString)))
      case value: PyDictionary =>
        val keys = value.keys()
        val mapped = for {
          i <- 0 until keys.size()
          key = keys.get(i)
          vle = value.get(key)
        } yield (key, vle)

        val v = mapped.map {
          case (a, b: PyObject) => convertAnyTypeResponse(b).map(a.toString -> _)
          case (a, b: Object) => Future.successful(a.toString -> Left(b.toString))
          case _ => Future.successful("A" -> Left("B"))
        }

        Future.sequence(v).map(l => List(Right(l.toMap)))

      case _ => Future.successful(List(Left("other")))
    }
  }

  private def convertResponse(value: PyObject, typeFilter: String, types: TypeCacheType): Future[RuleResultType] = {

    typeFilter match {
      case "int" =>
        value match {
          case pyInteger: PyInteger => Future.successful(List(Left(pyInteger.toString)))
          case null => Future.successful(List.empty)
          case other => Future.successful(List(Left("Invalid response type " + other.getType + ". Integer is required.")))
        }
      case "string" =>
        value match {
          case pyObject: PyObject => Future.successful(List(Left(pyObject.toString)))
          case null => Future.successful(List.empty)
        }
      case "anyType" =>
        convertAnyTypeResponse(value)

      case TypeDefs.isList(itemType) =>
        value match {
          case pySeq: PySequence =>
            val scalaIterable = JavaConverters.iterableAsScalaIterable(pySeq.asIterable())
            val fs = scalaIterable.map(item => convertResponse(item, itemType, types)).toList
            Future.sequence(fs).map(_.flatten)

          case null => Future.successful(List.empty)
          case other => Future.successful(List(Left("Invalid response type " + other.getType + ". List is required.")))
        }
      case otherType =>
        typeDefinitionService.typeDefinitionLookup(otherType, types).flatMap {
          case Some(typeDef) =>
            val b = typeDef.fields.flatMap {
              field => {
                try {
                  val pyObject = value match {
                    case pyDict: PyDictionary =>
                      val key = new PyString(field.name)
                      if (pyDict.has_key(key)) pyDict.get(key)
                      else throw new IllegalArgumentException("No such item in dictionary")
                    case _ => value.__getattr__(field.name)
                  }
                  List(convertResponse(pyObject, field.typeDef, types).map(a => field.name -> a))
                }
                catch {
                  case ex: Throwable =>
                    logger.error("Unable to read rule execution result", ex)
                    List()
                }
              }
            }
            Future.sequence(b).map(l => List(Right(l.toMap)))
          case None =>
            throw new IllegalStateException("No such type " + typeFilter)
        }
    }

  }
}

