package services

import javax.inject.{Inject, Singleton}

import models.repository.{DetailsRepositoryRec, ListRepository}
import persistence.repository.Repository

import scala.concurrent.Future

@Singleton
class RepositoryService @Inject()(repository: Repository) {

  def list(folderId: Option[Long] = None, currentUser: String, offset: Int, length: Int): Future[ListRepository] = {
    repository.list(folderId, currentUser, offset, length)
  }

  def remove(ids: Set[Long], currentUser: String): Future[Unit] = {
    repository.remove(ids, currentUser)
  }

  def details(id: Long, from: Int, length: Int, currentUser: String): Future[DetailsRepositoryRec] = {
    repository.details(id, from, length, currentUser)
  }

  def commit(currentUser: String): Future[Unit] = {
    repository.commit(currentUser)
  }

  def cancel(currentUser: String): Future[Unit] = {
    repository.cancel(currentUser)
  }

}
