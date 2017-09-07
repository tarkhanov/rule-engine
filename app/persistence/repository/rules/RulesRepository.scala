package persistence.repository.rules

import com.typesafe.scalalogging.StrictLogging
import models.repository.rules.RulesModel.RuleSet
import models.repository.rules.{RuleRepositoryRec, RuleRepositoryRecDetails, RulesModelXML}
import models.repository.{ActionRec, RepositoryRec}
import org.apache.commons.codec.digest.DigestUtils
import persistence.DBUtil
import persistence.repository.types.TypesRepository
import persistence.repository.{Repository, RepositoryExtension}
import slick.dbio.Effect.Read

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.matching.Regex
import scala.util.{Success, Try}

class RulesRepository(val repository: Repository, typesRepository: TypesRepository, dbUtil: DBUtil)
                     (implicit ec: ExecutionContext)
                      extends RepositoryExtension with StrictLogging {

  import repository.dbConfig._
  import profile.api._

  type RuleRepositoryExt = (Long, String, String)

  class RuleRepositoryTable(tag: Tag) extends Table[RuleRepositoryExt](tag, "REPOSITORY_RULE") {

    def id = column[Long]("ID", O.PrimaryKey)

    def repositoryFK = foreignKey("RuleRepository_FK", id, repository.repositoryTable)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

    def api = column[String]("API_VE")

    def definition = column[String]("DEFINITION")

    def * = (id, api, definition)
  }

  val ruleRepository = TableQuery[RuleRepositoryTable]

  def init(): Unit = {
    dbUtil.ifNoTable(ruleRepository)(db run ruleRepository.schema.create)
  }

  def create(newSeq: Option[String], newRec: RuleRepositoryRec, user: String): Future[Long] = {

    val ruleSet = RulesModelXML.parse(newRec.definition)
    val newApi = apiCode(ruleSet)

    val actions = for {
      (session, iq) <- repository.createNewEntryAction(newSeq, "rules", newRec.name, user)
      _ <- ruleRepository += (iq, newApi, newRec.definition)
    } yield iq

    db.run(actions.transactionally)
  }

  def get(id: Long): Future[Option[RuleRepositoryRec]] = {
    val query = repository.repositoryTable.filter(_.id === id).join(ruleRepository).on(_.id === _.id).take(1).result
    db.run(query).map(_.headOption.map(mapToRuleRec))
  }

  private val filterFormat: Regex = "(\\w+):([^;]*);?".r

  def lookup(filter: String): Future[Option[RuleRepositoryRec]] = {
    val properties = filterFormat.findAllIn(filter).matchData.map(g => g.group(1) -> g.group(2)).toMap
    val query = properties.get("seq") match {
      case Some(seq) =>
        properties.get("api") match {
          case Some(apiId) => // sequence + api
            repository.repositoryTable.join(ruleRepository).on(_.id === _.id).filter { case (rep, rule) => rep.seq === seq && rule.api === apiId }.sortBy(_._1.id.desc).result.headOption
          case None => // sequence + active
            repository.repositoryTable.join(ruleRepository).on(_.id === _.id).filter { case (rep, _) => rep.seq === seq && rep.active }.result.headOption
        }
      case None => // whole filter value is ID
        try {
          val longId = properties.getOrElse("id", filter).toLong
          repository.repositoryTable.filter(r => r.id === longId).join(ruleRepository).on(_.id === _.id).result.headOption
        }
        catch {
          case NonFatal(e) =>
            throw new IllegalArgumentException("Invalid format of filter used as ID", e)
        }
    }
    db.run(query.transactionally).map(_.map(mapToRuleRec))
  }

  private def mapToRuleRec(arg: (RepositoryRec, RuleRepositoryExt)): RuleRepositoryRec = arg match {
    case (rr, (_, a, d)) => RuleRepositoryRec(rr.id, rr.seq, a, rr.name, d)
  }

  def getRecordDetails(id: Long, user: String): Future[Option[RuleRepositoryRecDetails]] = {

    val query = for {
      sessionOption <- repository.getSessionOptionAction(user)
      recordOption <- repository.repositoryTable.filter(_.id === id).join(ruleRepository).on(_.id === _.id).take(1).result.headOption
      actions <- if (sessionOption.isDefined && recordOption.isDefined)
        repository.actionTable.filter(a => a.target === recordOption.get._1.id && a.session === sessionOption.get.id).result.map(_.toList)
      else
        DBIO.successful(List.empty)
    } yield (sessionOption, recordOption, actions)

    db.run(query.transactionally).map {
      case (_, Some(res), actions) => Some(RuleRepositoryRecDetails(mapToRuleRec(res), actions))
      case _ => None
    }
  }

  private def apiCode(ruleSet: RuleSet): String =
    DigestUtils.md5Hex((ruleSet.arguments, ruleSet.results).toString.getBytes)

  def saveDefinition(id: Long, ruleSet: RuleSet): Future[Try[Boolean]] = {
    logger.debug("Save Definition: " + ruleSet.toString)
    val definition = RulesModelXML.serialize(ruleSet).toString()
    // Api version signature
    val newApi = apiCode(ruleSet)
    val query = for {
      _ <- ruleSet.seq match {
        case Some(sequenceId) => repository.repositoryTable.filter(_.id === id).map(_.seq).update(sequenceId)
        case None => DBIO.successful(Unit)
      }
      _ <- repository.repositoryTable.filter(_.id === id).map(_.name).update(ruleSet.name)
      _ <- ruleRepository.filter(_.id === id).map(r => (r.api, r.definition)).update((newApi, definition))
    } yield ()
    db.run(query).map(_ => Success(true))
  }


  def getActiveRuleSetDefinition(seq: String): Future[Option[RuleRepositoryRec]] = {
    val query = repository.repositoryTable.filter(r => r.seq === seq && r.active).join(ruleRepository).on(_.id === _.id).result.headOption
    db.run(query).map(_.map(mapToRuleRec))
  }

  // Validation

  override def validateOnCommit(record: RepositoryRec, action: ActionRec): DBIOAction[Any, NoStream, Read with Effect] = {
    if (record.kind == "rules") {
      for {
        rec <- ruleRepository.filter(_.id === record.id).result.head.map(mapToRuleRec(record, _))
        _ <- {
          logger.debug("Committing rule: " + rec)
          val typeD = RulesModelXML.parse(rec.definition)
          val dependencies = (typeD.arguments.list.map(_.`type`) ++ typeD.results.list.map(_.`type`)).toSet.flatMap(typesRepository.subtypes)
          logger.debug("Dependencies: " + dependencies)
          val listOfQueries = dependencies.toList.flatMap(typesRepository.validateTypeQuery("rule " + record.name))
          DBIO.seq(listOfQueries: _*)
        }
      } yield Unit
    }
    else
      DBIO.successful(Unit)
  }
}
