package services.configuration

import java.io.InputStream
import javax.inject.Inject

import services.configuration.ConfigurationService.UploadStatus
import services.rules.RulesService
import services.types.TypesService

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object ConfigurationService {

  case class UploadStatus(id: Long, message: String)

}

class ConfigurationService @Inject()(typesService: TypesService, rulesService: RulesService)(implicit ec: ExecutionContext) {

  private val configurationFilters: Seq[ConfigurationFilter] = List(typesService, rulesService)

  def process(name: String, stream: => InputStream, user: String): Future[List[Try[UploadStatus]]] = {
    val filters = configurationFilters.map(_.applyConfigurationFilter(name, stream, user))
    Future.sequence(filters).map(_.flatten.toList)
  }

}
