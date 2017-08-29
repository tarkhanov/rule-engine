package persistence.repository

import models.repository.{ActionRec, RepositoryRec}
import slick.dbio.Effect.{Read, Write}
import slick.dbio.{DBIOAction, NoStream}

trait RepositoryExtension {
  def validateOnCommit(record: RepositoryRec, action: ActionRec): DBIOAction[Any, NoStream, Read with Write]
}
