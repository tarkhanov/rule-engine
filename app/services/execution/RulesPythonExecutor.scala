package services.execution

import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.StrictLogging
import models.repository.rules.RulesModel._
import models.repository.rules.RulesModelXML
import models.repository.types.TypeRepositoryRec
import models.repository.types.TypesModel.{Type, TypeDefs}
import org.python.core._
import org.python.util.PythonInterpreter
import services.execution.RulesPythonExecutor._
import services.types.TypesCacheService
import services.types.TypesCacheService.TypeCacheType

import scala.collection.JavaConverters
import scala.collection.immutable.Seq
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

/**
 * Created by Sergey Tarkhanov on 6/9/2015.
 */

object RulesPythonExecutor {

  type RequestDataValueType = Seq[Either[String, AnyRef]]
  type RequestDataType = Map[String, RequestDataValueType]

  case class ExecutionResult[T](value: T, exceptions: List[Throwable] = List.empty)

  type ConditionResult = ExecutionResult[Boolean]
  type BodyResult = ExecutionResult[List[(String, Future[RuleResultType])]]

  type RuleResultType = List[Either[String, Map[String, AnyRef]]]

}

@Singleton
class RulesPythonExecutor @Inject()(typeDefinitionService: TypesCacheService, actorSystem: ActorSystem)(implicit ec: ExecutionContext) extends StrictLogging {

  private val proxyFactory = actorSystem.actorOf(CreatePyProxyActor.props(getClass.getClassLoader), "PythonStructureProxyActor")

  private implicit val timeout: Timeout = Timeout(5 seconds)

  def createProxy(name: String, structure: PythonStructure): Future[PyProxy] =
    (proxyFactory ask CreatePyProxy(name, structure)).mapTo[PyProxy]

  // Execute ------------------------------------------------------------

  private def resolveTypeIdToPyObject(name: String, typeId: String, requestDataValues: String => RequestDataValueType, types: TypeCacheType): Future[Seq[PyObject]] = {
    typeId match {
      case "int" =>
        val integerValue = Try(requestDataValues(name).flatMap(_.left.toOption.map(v => Integer.parseInt(v))))
        integerValue match {
          case Success(value) => Future.successful(value.map { i => new PyInteger(i) })
          case Failure(ex) => Future.failed(new IllegalArgumentException("Unable to parse integer value", ex))
        }

      case "string" =>
        val stringValue = requestDataValues(name).flatMap(_.left.toOption)
        Future.successful(stringValue.map { s => new PyString(s) })

      case TypeDefs.isList(itemType) =>
        val list: Seq[Future[Seq[PyObject]]] = requestDataValues(name + "s").map {
          i => {
            val is = i.right.map(_.asInstanceOf[RequestDataType]).right.toOption
            val getLevelItems = (name: String) => is.map(_.getOrElse(name, Seq.empty)).getOrElse(Seq.empty)
            resolveTypeIdToPyObject(name, itemType, getLevelItems, types)
          }
        }
        def convertSeqToPyList(spo: Seq[PyObject]) = new PyList(JavaConverters.asJavaCollection(spo))
        Future.sequence(list).map(_.map(convertSeqToPyList))

      case otherType =>
        typeDefinitionService.typeDefinitionLookup(otherType, types).flatMap {
          case Some((record: TypeRepositoryRec, typeDef: Type)) =>
            val mapOfFields = requestDataValues(name).headOption.map(_.right.get.asInstanceOf[RequestDataType])
            val pyFieldsFutures = typeDef.fields.map(f => {
              def getLevelField(fieldName: String) = mapOfFields.map(_.getOrElse(fieldName, Seq.empty).take(1)).getOrElse(Seq.empty) // Get 1st argument of structure
              resolveTypeIdToPyObject(f.name, f.typeDef, getLevelField, types).map(f.name -> _.headOption.orNull)
            })
            Future.sequence(pyFieldsFutures).flatMap {
              pyFields => createProxy("PyProxy_" + typeDef.name + "_" + record.id, pyFields.toMap).map(Seq(_))
            }
          case _ =>
            throw new IllegalStateException("Type not found: " + otherType)
        }
    }
  }

  private def generateArguments(requestData: RequestDataType, arguments: List[Argument], types: TypeCacheType): Future[Map[String, PyObject]] = {
    val futures = arguments.map(arg => {
      val values = (argName: String) => requestData.getOrElse(argName, Seq.empty).take(1)
      resolveTypeIdToPyObject(arg.name, arg.`type`, values, types).map(arg.name -> _)
    })
    Future.sequence(futures).map(_.toMap.mapValues(_.headOption.orNull))
  }

  def execute(requestData: RequestDataType, id: Long, definition: String): Future[List[(Rule, ConditionResult, Option[BodyResult])]] = {

    if (logger.underlying.isDebugEnabled) {
      logger.debug("Execute RuleSet with id = {}", id)
      for ((key, value) <- requestData) {
        logger.debug("Argument {} = {}", key, value.mkString(", "))
      }
    }

    val rulesDef = RulesModelXML.parse(definition)
    val arguments = rulesDef.arguments.list
    val types = typeDefinitionService.newTypeCache
    val argsFuture = generateArguments(requestData, arguments, types)
    argsFuture.map(args => rulesDef.rules.list.map(rule => executeRule(rule, args, rulesDef.results.list, types)))
  }

  private def executeRule(rule: Rule, pythonArgs: Map[String, PyObject], results: List[Result], types: TypeCacheType): (Rule, ConditionResult, Option[BodyResult]) = {

    logger.debug("Execute Rule '{}'", rule.name.getOrElse("[Untitled]"))

    val pi = new PythonInterpreter()
    pythonArgs.foreach { case (name, value) => if (value != null) pi.set(name, value) }

    val conditionResult = try {
      val result = pi.eval(rule.condition.code.trim)
      logger.debug("Invocation result is {}", result)
      ExecutionResult(result.equals(new PyBoolean(true)))
    }
    catch {
      case ex: Throwable =>
        logger.debug("Exception during rule invocation: {}, {}", ex.getClass.getName, ex.getMessage)
        ExecutionResult(false, List(ex))
    }

    val bodyResult = if (conditionResult.value) {
      val result: BodyResult = try {
        pi.exec(rule.body.code.trim)
        ExecutionResult(results.map(r => r.name -> convertResponse(pi.get(r.name), r.`type`, types)))
      }
      catch {
        case ex: Throwable => ExecutionResult(List(), List(ex))
      }
      Some(result)
    }
    else
      None

    (rule, conditionResult, bodyResult)
  }

  def convertAnyTypeResponse(value: PyObject): Future[RuleResultType] = {
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

  def convertResponse(value: PyObject, typeFilter: String, types: TypeCacheType): Future[RuleResultType] = {

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
          case Some((_: TypeRepositoryRec, typeDef: Type)) =>
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
                  case _: Throwable => List()
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