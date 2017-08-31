package services.types

import java.io.StringReader
import javax.inject.{Inject, Singleton}

import models.repository.types.{TypeModelXML, TypeRepositoryRec}
import models.repository.types.TypesModel.{Type, TypeDefs}
import services.types.TypeDefinitionService.TypeCacheType

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.xml.XML

object TypeDefinitionService {
  type TypeCacheType = mutable.Map[String, (TypeRepositoryRec, Type)]
}

@Singleton
class TypeDefinitionService @Inject()(typesService: TypesService)(implicit ec: ExecutionContext) {

  def newTypeCache: TypeCacheType = mutable.Map[String, (TypeRepositoryRec, Type)]()

  private def createCacheItem(filter: String, typeCache: TypeCacheType)(record: TypeRepositoryRec) = {
    val fields = TypeModelXML.readFields(XML.load(new StringReader(record.definition)))
    val typeDef = Type(Some(record.seq), record.name, fields)
    val pair = (record, typeDef)
    typeCache += filter -> pair
    typeDef
  }

  def typeDefinitionLookup(filter: String, typeCache: TypeCacheType): Future[Option[Type]] = {
    filter match {
      case TypeDefs.isSequence(seq) =>
        val cached = typeCache.get(filter)
        if (cached.isEmpty)
          typesService.lookupSeq(seq).map(_.map(createCacheItem(filter, typeCache)))
        else
          Future.successful(cached.map { case (_, t) => t } )

      case TypeDefs.isIdentifier(id) =>
        Try(id.toLong) match {
          case Success(longId) =>
            val cached = typeCache.get(filter)
            if (cached.isEmpty)
              typesService.lookupId(longId).map(_.map(createCacheItem(filter, typeCache)))
            else
              Future.successful(cached.map { case (_, t) => t } )

          case Failure(_) =>
            Future.failed(new IllegalArgumentException("Invalid id format: " + id))
        }

      case _ => Future.failed(new IllegalStateException("Condition \"" + filter + "\" can't be processed"))
    }
  }

}
