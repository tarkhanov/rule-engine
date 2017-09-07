package services.types
import javax.inject.{Inject, Singleton}

import models.repository.types.{TypeRepositoryRec, TypeRepositoryRecDetails}
import persistence.repository.Repository

import scala.concurrent.Future
import scala.util.Try

class TypesService @Inject()(repository: Repository) extends ConfigureTypesService {

  private val typesRepository = repository.typesRepository

  def lookupId(longId: Long): Future[Option[TypeRepositoryRec]] =
    typesRepository.lookupById(longId)

  def lookupSeq(seq: String): Future[Option[TypeRepositoryRec]] =
    typesRepository.lookupBySeq(seq)

  override def create(newSeq: Option[String], newRec: TypeRepositoryRec, user: String): Future[Long] =
    typesRepository.create(newSeq, newRec, user)

  def getRecordDetails(id: Long, user: String): Future[Option[TypeRepositoryRecDetails]] =
    typesRepository.getRecordDetails(id, user)

  def saveDefinition(id: Long, definition: String, seq: Option[String] = None, name: Option[String] = None): Future[Try[Boolean]] =
    typesRepository.saveDefinition(id, definition, seq, name)

}
