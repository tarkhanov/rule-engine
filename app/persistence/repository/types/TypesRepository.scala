package persistence.repository.types

import java.io.StringReader

import com.typesafe.scalalogging.StrictLogging
import models.repository.types.TypesModel.TypeDefs
import models.repository.types.{TypeModelXML, TypeRepositoryRec, TypeRepositoryRecDetails}
import models.repository.{ActionRec, RepositoryRec}
import persistence.DBUtil
import persistence.repository.{Repository, RepositoryExtension}
import slick.dbio.Effect.Read

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.xml.XML

class TypesRepository(val repository: Repository, dbUtil: DBUtil)
                     (implicit ec: ExecutionContext)
                      extends RepositoryExtension with StrictLogging {

  import repository.dbConfig._
  import profile.api._

  type TypeRepositoryExt = (Long, String)

  class TypeRepositoryTable(tag: Tag) extends Table[TypeRepositoryExt](tag, "REPOSITORY_TYPE") {

    def id = column[Long]("ID", O.PrimaryKey)

    def supplier = foreignKey("TypeRepository_FK", id, repository.repositoryTable)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

    def definition = column[String]("DEFINITION")

    def * = (id, definition)
  }

  val typeTable = TableQuery[TypeRepositoryTable]

  def init(): Unit = {
    dbUtil.ifNoTable(typeTable)(db run typeTable.schema.create)
  }

  def create(newSeq: Option[String], newRec: TypeRepositoryRec, user: String): Future[Long] = {
    val actions = for {
      (session, iq) <- repository.createNewEntryAction(newSeq, "types", newRec.name, user)
      _ <- typeTable +=(iq, newRec.definition)
    } yield iq
    db.run(actions.transactionally)
  }

  def lookupBySeq(seq: String): Future[Option[TypeRepositoryRec]] = {
    val query = repository.repositoryTable.filter(r => r.seq === seq && r.active).join(typeTable).on(_.id === _.id).result.headOption
    db.run(query.transactionally).map(_.map(mapToTypeRec))
  }

  def lookupById(id: Long): Future[Option[TypeRepositoryRec]] = {
    val query = repository.repositoryTable.filter(_.id === id).join(typeTable).on(_.id === _.id).result.headOption
    db.run(query.transactionally).map(_.map(mapToTypeRec))
  }

  private def mapToTypeRec(arg: (RepositoryRec, TypeRepositoryExt)): TypeRepositoryRec = arg match {
    case (rr, (_, d)) => TypeRepositoryRec(rr.id, rr.seq, rr.name, d)
  }

  def getRecordDetails(id: Long, user: String): Future[Option[TypeRepositoryRecDetails]] = {
    val query = for {
      sessionOption <- repository.getSessionOptionAction(user)
      recordOption <- repository.repositoryTable.filter(_.id === id).join(typeTable).on(_.id === _.id).take(1).result.headOption
      actions <- if (sessionOption.isDefined && recordOption.isDefined)
        repository.actionTable.filter(a => a.target === recordOption.get._1.id && a.session === sessionOption.get.id).result.map(_.toList)
      else
        DBIO.successful(List.empty)
    } yield (sessionOption, recordOption, actions)
    db.run(query.transactionally).map {
      case (_, Some(res), actions) => Some(TypeRepositoryRecDetails(mapToTypeRec(res), actions))
      case _ => None
    }
  }

  def saveDefinition(id: Long, definition: String, seq: Option[String] = None, name: Option[String] = None): Future[Try[Boolean]] = {
    val query = for {
      _ <- seq.map(value => repository.repositoryTable.filter(_.id === id).map(_.seq).update(value)).getOrElse(DBIO.successful(Unit))
      _ <- name.map(value => repository.repositoryTable.filter(_.id === id).map(_.name).update(value)).getOrElse(DBIO.successful(Unit))
      _ <- typeTable.filter(_.id === id).map(r => r.definition).update(definition)
    } yield ()
    db.run(query).map(_ => Success(true))
  }

  // Validation

  def subtypes(typeDef: String): Set[String] = typeDef match {
    case TypeDefs.isList(item) =>
      Set(item)
    case _ =>
      Set(typeDef)
  }

  @tailrec
  final def validateTypeQuery(rootResourceTitle: String)(condition: String): List[DBIOAction[Unit, NoStream, Read with Effect]] = {

    def validateQueryResult(cond: repository.RepositoryTable => Rep[Boolean]): DBIOAction[Unit, NoStream, Read with Effect] =
      repository.repositoryTable.filter(r => cond(r) && r.kind === "types").map(_.id).result.headOption.flatMap {
        case Some(id) =>
          logger.debug("Type dependency " + condition + " resolved as type id " + id)
          DBIO.successful(Unit)
        case None =>
          DBIO.failed(new IllegalStateException("Dependency " + condition + " unresolved while committing "))
      }

    condition match {
      case "int" | "string" | "anyType" =>
        List.empty
      case TypeDefs.isSequence(seq) =>
        List(validateQueryResult(r => r.seq === seq && r.active))
      case TypeDefs.isIdentifier(id) =>
        Try(id.toLong) match {
          case Success(longId) =>
            List(validateQueryResult(r => r.id === longId))
          case Failure(_) =>
            throw new IllegalArgumentException("Invalid id format: " + id)
        }
      case TypeDefs.isList(item) =>
        validateTypeQuery(rootResourceTitle)(item)
      case _ =>
        throw new IllegalArgumentException("Type \"" + condition + "\" was not found for " + rootResourceTitle)
    }
  }

  override def validateOnCommit(record: RepositoryRec, action: ActionRec): DBIOAction[Any, NoStream, Read with Effect] =
    if (record.kind == "types") for {
      rec <- typeTable.filter(_.id === record.id).result.head.map(mapToTypeRec(record, _))
      _ <- {
        logger.debug("Committing type: " + rec.name)
        val typeD = TypeModelXML.readFields(XML.load(new StringReader(rec.definition)))
        val dependencies = typeD.map(_.typeDef).toSet
        logger.debug("Dependencies: " + dependencies)
        val listOfQueries = dependencies.toList.flatMap(validateTypeQuery("type " + record.name))
        DBIO.seq(listOfQueries: _ *)
      }
    } yield Unit
    else
      DBIO.successful(Unit)

}