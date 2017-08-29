package persistence.users

import javax.inject.{Inject, Singleton}

import com.typesafe.scalalogging.StrictLogging
import models.users.User
import persistence.DBUtil
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserRepository @Inject()(dbConfigProvider: DatabaseConfigProvider, dbUtil: DBUtil)(implicit ec: ExecutionContext) extends StrictLogging {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  private class UserTable(tag: Tag) extends Table[User](tag, "USER") {
    def login = column[String]("USERNAME", O.PrimaryKey, O.Length(50))
    def salt = column[String]("SALT", O.Length(50))
    def password = column[String]("PASSWORD", O.Length(50))
    def * = (login, salt, password) <> (User.tupled, User.unapply)
  }

  private val users = TableQuery[UserTable]

  def init(): Unit = {
    dbUtil.ifNoTableOf(users)(db run users.schema.create)
  }

  init()

  def getUser(login: String): Future[Option[User]] =
    db run users.filter(_.login === login).take(1).result.map(_.headOption)

  def create(user: User): Future[Unit] =
    db run (users += user) map (_ => Unit)

  def list(offset: Int, length: Int): Future[Seq[User]] =
    db run users.sorted(_.login).drop(offset).take(length).result

  def count: Future[Int] =
    db run users.length.result

}
