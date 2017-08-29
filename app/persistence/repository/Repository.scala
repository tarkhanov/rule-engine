package persistence.repository

import java.sql.Timestamp
import java.util.Date
import javax.inject.{Inject, Singleton}

import com.typesafe.scalalogging.StrictLogging
import models.repository._
import org.hashids.Hashids
import persistence.DBUtil
import play.api.db.slick.DatabaseConfigProvider
import slick.dbio.Effect.{Read, Write}
import slick.driver.JdbcProfile
import persistence.repository.Repository._
import persistence.repository.rules.RulesRepository
import persistence.repository.types.TypesRepository
import play.api.Play

import scala.concurrent.{ExecutionContext, Future}

object Repository {
  val CREATE_ACTION = "CREATE"
  val REMOVE_ACTION = "REMOVE"
  val COMMIT_ACTION = "COMMIT"
}

@Singleton
class Repository @Inject()(dbConfigProvider: DatabaseConfigProvider, dbUtil: DBUtil)(implicit ec: ExecutionContext) extends StrictLogging {

  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import driver.api._

  class ActionTable(tag: Tag) extends Table[ActionRec](tag, "REPOSITORY_ACTION") {

    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def action = column[String]("ACTION", O.Length(50))
    def target = column[Long]("TARGET")
    def session = column[Long]("SESSION")

    def * = (id, action, target, session) <> (ActionRec.tupled, ActionRec.unapply)
  }

  class RepositoryTable(tag: Tag) extends Table[RepositoryRec](tag, "REPOSITORY") {

    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def seq = column[String]("SEQ", O.Length(50))
    def kind = column[String]("KIND", O.Length(50))
    def name = column[String]("NAME", O.Length(50))
    def active = column[Boolean]("ACTIVE")
    def parentId = column[Option[Long]]("PARENT_ID")

    def * = (id, seq, kind, name, active, parentId) <> (RepositoryRec.tupled, RepositoryRec.unapply)
  }

  class SequenceTable(tag: Tag) extends Table[(Long, Long)](tag, "REPOSITORY_SEQUENCE") {

    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def value = column[Long]("VALUE")

    def * = (id, value)
  }

  class SessionTable(tag: Tag) extends Table[SessionRec](tag, "REPOSITORY_SESSION") {

    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def user = column[String]("USER", O.Length(50))
    def creationDate = column[Timestamp]("CREATION_DATE")
    def commitDate = column[Option[Timestamp]]("COMMIT_DATE")

    def * = (id, user, creationDate, commitDate) <> (SessionRec.tupled, SessionRec.unapply)
  }

  def now = new Timestamp(new Date().getTime)

  val repositoryTable = TableQuery[RepositoryTable]
  val actionTable = TableQuery[ActionTable]
  val sessionTable = TableQuery[SessionTable]
  val sequenceTable = TableQuery[SequenceTable]

  val typesRepository = new TypesRepository(this, dbUtil)
  val rulesRepository = new RulesRepository(this, typesRepository, dbUtil)

  def init(): Unit = {
    dbUtil.ifNoTableOf(repositoryTable)(db run repositoryTable.schema.create)
    dbUtil.ifNoTableOf(actionTable)(db run actionTable.schema.create)
    dbUtil.ifNoTableOf(sessionTable)(db run sessionTable.schema.create)
    dbUtil.ifNoTableOf(sequenceTable)(db run sequenceTable.schema.create)
    typesRepository.init()
    rulesRepository.init()
  }

  init()

  /**
   * List operation
   */
  def list(folderId: Option[Long] = None, currentUser: String, offset: Int, length: Int): Future[ListRepository] = {

    def actionsAndSessions(sessionId: Long) =
      createActionQuery.filter(_.session === sessionId)

    def recordsToDisplayListOfGroups(sessionId: Long) = {
      val joinFilesAndSessions = filesInCurrentFolder(folderId) joinLeft actionsAndSessions(sessionId) on { case (f, a) => f.id === a.target }
      joinFilesAndSessions filter { case (f, a) => f.active || a.isDefined }
    }

    /* Get list of records to be displayed */
    def recordsToDisplay(sessionIdOption: Option[Long]) = {
      val query = sessionIdOption match {
        case Some(sessionId) =>
          recordsToDisplayListOfGroups(sessionId) map { case (f, _) => f }
        case None =>
          filesInCurrentFolder(folderId) filter (_.active)
      }
      // Extract max item and count of items in sequences
      query groupBy (_.seq) map { case (seq, group) => (seq, group.map(_.id).max, group.map(_.id).countDistinct) }
    }

    /* Get count of items to be displayed */
    def recordsToDisplayForCount(sessionIdOption: Option[Long]) = sessionIdOption match {
      case Some(sessionId) =>
        recordsToDisplayListOfGroups(sessionId) map { case (f, _) => f.seq } countDistinct
      case None =>
        filesInCurrentFolder(folderId) filter (_.active) map (_.seq) countDistinct // groupBy (_.seq) map { case (seq, group) => seq }
    }

    def actionAndSessionTable(files: Traversable[Long]) =
      actionTable.filter(_.target inSet files) join sessionTable on (_.session === _.id)

    val displayActions = Set(CREATE_ACTION, REMOVE_ACTION)

    val query = for {
      session <- getSessionOptionAction(currentUser)
      records <- recordsToDisplay(session.map(_.id)).join(repositoryTable).on { case ((seq, ver, count), f) => seq === f.seq && ver === f.id }.sortBy { case (_, f) => f.name }.drop(offset).take(length).result
      recordsCount <- recordsToDisplayForCount(session.map(_.id)).result
      ids = records.map { case (_, f) => f.id }
      actions <- actionAndSessionTable(ids).filter { case (a, s) => s.user === currentUser && a.action.inSet(displayActions) }.map { case (a, _) => a }.result
      sequences = records.map { case (_, f) => f.seq }
      actionUsers <- repositoryTable.filter(f => f.seq inSet sequences).join(actionTable.join(sessionTable.filter(s => s.user =!= currentUser && s.commitDate.isEmpty)) on (_.session === _.id)).on { case (f, (a, _)) => f.id === a.target }.map { case (f, (_, s)) => (f.seq, s.user) }.result
    } yield (session, records, recordsCount, actions, actionUsers)

    db.run(query.transactionally) map {
      case (session, metaAndFiles, recordsCount, actions, actionUsers) =>
        val actionBuffer = actions.groupBy(_.target)
        val foreignersBuffer = actionUsers.groupBy { case (seq, _) => seq }.mapValues(s => s.map{ case (_, user) => user })
        val recs = metaAndFiles.map {
          case ((_, _, count), file) =>
            val fileActions: List[ActionRec] = actionBuffer.get(file.id).map(_.toList).getOrElse(List.empty)
            val fileForeigners: Set[String] = foreignersBuffer.get(file.seq).map(_.toSet).getOrElse(Set.empty)
            ListRepositoryRec(file, count, fileActions, fileForeigners)
        }
        ListRepository(session, recs, recordsCount)
    }
  }

  /**
   * Commit operation
   */
  def commit(currentUser: String): Future[Unit] = {

    def activatePreviousRecord(record: RepositoryRec) =
      for {
        previousItem <- repositoryTable.filter(r => r.seq === record.seq && r.id < record.id).sortBy(_.id.desc).take(1).map(_.id).result
        _ <- repositoryTable.filter(_.id inSet previousItem).map(_.active).update(true)
      } yield ()

    def commitOneRecordActions(pair: (ActionRec, RepositoryRec)): DBIOAction[Unit, NoStream, Read with Write] = {
      val (action, record) = pair
      action.action match {
        case CREATE_ACTION =>
          logger.trace("Commit create action: " + action)
          for {
            _ <- repositoryTable.filter(r => r.seq === record.seq && r.active).map(_.active).update(false) // deactivate
            _ <- DBIO.seq(repositories.map(_.validateOnCommit(record, action)).toList: _ *)
            _ <- repositoryTable.filter(r => r.id === record.id).map(r => r.active).update(true) // activate
            _ <- actionTable.filter(_.id === action.id).delete
            _ <- actionTable += ActionRec(0L, COMMIT_ACTION, action.target, action.session)
          } yield ()
        case REMOVE_ACTION =>
          logger.trace("Commit remove action: " + action)
          for {
            _ <- activatePreviousRecord(record) if record.active
            _ <- repositoryTable.filter(r => r.id === record.id).delete
            _ <- actionTable.filter(_.id === action.id).delete
          } yield ()
      }
    }

    val query = for {
      session <- getSessionIdOrExceptionAction(currentUser)
      actionTableQuery = actionTable filter (_.session === session) sortBy (_.id.asc) sortBy (_.target)
      actions <- actionTableQuery.join(repositoryTable).on((r1, r2) => r1.target === r2.id).result
      _ <- DBIO.seq(actions map commitOneRecordActions: _*)
      _ <- sessionTable filter (_.id === session) map (_.commitDate) update Some(now)
    } yield ()

    db.run(query.transactionally)
  }

  private val repositories = Set(typesRepository, rulesRepository)

  /**
    * Cancel operation
    */
  def cancel(currentUser: String): Future[Unit] = {
    // Records which was created during current session
    val records = for {
      session <- getSessionIdOrExceptionAction(currentUser)
      query <- actionTable.filter(r => r.session === session && r.action === CREATE_ACTION).map(_.target).result
      _ <- repositoryTable.filter(_.id.inSet(query)).delete
      // Actions made during current session
      _ <- sessionTable.filter(_.id === session).delete
    } yield ()
    // Run transaction
    db.run(records.transactionally)
  }

  /**
   * Remove operation
   */
  def remove(ids: Set[Long], currentUser: String): Future[Unit] = {

    def addRemoveActionToRecord(recordId: Long, session: Long) =
      for {
        action <- actionTable.filter(a => a.target === recordId && a.action === REMOVE_ACTION).take(1).result
        _ <- actionTable += ActionRec(0L, REMOVE_ACTION, recordId, session) if action.isEmpty
      } yield ()

    val query = for {
      session <- autoCreateSessionIdAction(currentUser)
      // Get ids of files created during current session
      newItemsToRemove <- repositoryTable.filter(_.id.inSet(ids)).join(createActionQuery.filter(_.session === session)).on(_.id === _.target).map { case (f, _) => f.id }.result
      // Remove files created during current session
      _ <- repositoryTable.filter(_.id inSet newItemsToRemove).delete
      // if no CREATE action in current session
      // if no actions in any session
      oldActions <- actionTable.filter(a => a.action === CREATE_ACTION && a.target.inSet(ids) && a.session === session).map(_.target).result
      oldLocked <- actionTable.filter(a => a.target.inSet(ids) && a.session =!= session).join(sessionTable.filter(_.commitDate.isEmpty)).on(_.session === _.id).map { case (a, _) => a.target }.result
      _ <- DBIO.seq(ids.diff(oldActions.toSet).diff(oldLocked.toSet).map(a => addRemoveActionToRecord(a, session)).toSeq: _*)
    } yield ()
    // Run transaction
    db.run(query.transactionally)
  }

  /**
   * Details, for UI
   */
  def details(id: Long, from: Int, length: Int, currentUser: String): Future[DetailsRepositoryRec] = {

    val query = for {
      session <- getSessionOptionAction(currentUser)
      currentOption <- repositoryTable.filter(_.id === id).result.headOption
      current = currentOption.getOrElse(throw new IllegalArgumentException("Not found"))
      history <- repositoryTable.filter(_.seq === current.seq).sortBy(_.id.desc).drop(from).take(length).result
      historyCount <- repositoryTable.filter(_.seq === current.seq).map(_.id).countDistinct.result
      actions <- actionTable.filter(_.target inSet history.map(_.id)).sortBy(_.id.asc).join(sessionTable).on(_.session === _.id).result
    } yield (session, current, history, historyCount, actions)

    db.run(query.transactionally).map {
      case (session, current, history, historyCount, actions) =>
        val buffer = actions.groupBy { case (action, _) => action.target }.mapValues(_.toList)
        val newHistory = history map { h => DetailsRepositoryItem(h, buffer.getOrElse(h.id, List.empty)) }
        DetailsRepositoryRec(session, current, newHistory, historyCount)
    }
  }

  /**
    * Queries
    */
  def userSessionQuery(currentUser: String): Query[SessionTable, SessionRec, scala.Seq] =
    sessionTable.filter(s => s.user === currentUser && s.commitDate.isEmpty).take(1)

  def createActionQuery =
    actionTable.filter(_.action === Repository.CREATE_ACTION)

  def actionsJoinSessionsQuery(sessions: Query[SessionTable, SessionRec, Seq]): Query[(ActionTable, SessionTable), (ActionRec, SessionRec), Seq] =
    createActionQuery join sessions on (_.session === _.id)

  def filesInCurrentFolder(currentFolderId: Option[Long]): Query[RepositoryTable, RepositoryRec, Seq] =
    if (currentFolderId.isEmpty)
      repositoryTable filter (_.parentId.isEmpty)
    else
      repositoryTable filter (a => a.parentId.isDefined && a.parentId.get === currentFolderId.get)


  /**
    * DB Actions
    */
  def autoCreateSessionIdAction(user: String): DBIOAction[Long, NoStream, Read with Write] = {
    val query = userSessionQuery(user).map(_.id).result.map(_.headOption)
    query.flatMap {
      case Some(long) => query.map(_.get)
      case None => sessionTable.returning(sessionTable.map(_.id)) += SessionRec(0L, user, now)
    }
  }

  def getSessionIdOrExceptionAction(user: String): DBIOAction[Long, NoStream, Read] =
    userSessionQuery(user).map(_.id).result.map(_.headOption.getOrElse(throw new IllegalStateException(s"No active session for user: $user")))

  def getSessionOptionAction(user: String): DBIOAction[Option[SessionRec], NoStream, Read] =
    userSessionQuery(user).result.map(_.headOption)

  /**
    * Create new entry action
    */

  val hashIds = Hashids(Play.current.configuration.getString("play.crypto.secret").get)

  def createNewEntryAction(newSeq: Option[String], kind: String, name: String, user: String, parentId: Option[Long] = None): DBIOAction[(Long, Long), NoStream, Read with Write with Effect] = {

    def newSequenceAction = (sequenceTable.returning(sequenceTable.map(_.id)) +=(0L, 1L)).map(id => "CM" + hashIds.encode(id))

    for {
      sessionId <- autoCreateSessionIdAction(user)
      newSequenceValidated <- {
        if (newSeq.isDefined) {
          if (newSeq.get == "new") {
            logger.debug("Sequence is defined as new")
            newSequenceAction
          }
          else {
            logger.debug("Sequence is defined")
            DBIO.successful(newSeq.get)
          }
        }
        else {
          for {
            lastSubmittedItem <- {
              logger.debug("Find last submitted item by name which will be overwritten")
              repositoryTable.filter(r => r.active === true && r.name === name).result
            }
            newSequenceId <- if (lastSubmittedItem.length == 1) {
              logger.debug("Sequence is NOT defined, then use sequence id for currently submitted item with the same name (Overwrite by name)")
              DBIO.successful(lastSubmittedItem.head.seq)
            }
            else if (lastSubmittedItem.length > 1) {
              logger.debug("Sequence not specified but multiple records with same name are active in repository")
              DBIO.failed(new IllegalStateException("Sequence not specified but multiple records with same name are active in repository"))
            }
            else  {
              logger.debug("Sequence is NOT defined, no currently submitted items with such name to overwrite, (New Sequence Id)")
              newSequenceAction
            }
          } yield newSequenceId
        }
      }
      result <- {
        logger.debug("Find uncommitted version created by another user. In this case, rise an error")
        repositoryTable.filter(_.seq === newSequenceValidated).join(actionTable.filter(a => a.action === Repository.CREATE_ACTION && a.session =!= sessionId)).on(_.id === _.target).map { case (rec, _) => rec.id }.result
      }
      _ <- {
        if (result.nonEmpty)
          DBIO.failed(new IllegalStateException("There are uncommitted changes created for this sequence by another user"))
        else
          DBIO.successful(Unit)
      }
      currentlyModifiedRecord <- {
        logger.debug("Find version which is not committed but must be overwritten by new")
        repositoryTable.filter(_.seq === newSequenceValidated).join(actionTable.filter(a => a.action === Repository.CREATE_ACTION && a.session === sessionId)).on(_.id === _.target).map { case (rec, _) => rec.id }.result
      }
      _ <- {
        if (currentlyModifiedRecord.isEmpty) {
          logger.debug("No action, current item not modified in this session")
          DBIO.successful(Unit)
        }
        else {
          logger.debug("Discard items which are not committed but with same seq id")
          repositoryTable.filter(_.id inSet currentlyModifiedRecord).delete
        }
      }
      iq <- repositoryTable.returning(repositoryTable map (_.id)) += RepositoryRec(0L, newSequenceValidated, kind, name, active = false)
      _ <- actionTable += ActionRec(0L, Repository.CREATE_ACTION, iq, sessionId)
    } yield (sessionId, iq)
  }
}
