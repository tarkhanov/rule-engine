package persistence

import javax.inject.{Inject, Singleton}

import com.typesafe.scalalogging.StrictLogging
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.jdbc.meta.MTable
import slick.lifted.AbstractTable

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

@Singleton
class DBUtil @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) extends StrictLogging {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import driver.api._

  private lazy val list = Await.result(db run MTable.getTables, 5.seconds)

  def ifNoTableOf[A <: AbstractTable[_]](table: TableQuery[A])(block: => Unit): Unit = {
    val exists = list.exists(_.name.name.compareToIgnoreCase(table.baseTableRow.tableName) == 0)
    if (!exists) {
      logger.info("Creating table " + table.baseTableRow.tableName + "...")
      block
    }
  }


}
