package modules

import com.google.inject.AbstractModule
import persistence.repository.Repository
import services.RepositoryService
import services.configuration.ConfigurationService
import services.execution.RulesPythonExecutor
import services.rules.RulesService
import services.types.{TypeDefinitionService, TypesService}

class RepositoryModule extends AbstractModule {

  override def configure(): Unit = {

    bind(classOf[Repository])
    bind(classOf[RepositoryService])

    bind(classOf[ConfigurationService])
    bind(classOf[RulesService])
    bind(classOf[TypesService])
    bind(classOf[RepositoryService])

    bind(classOf[TypeDefinitionService])
    bind(classOf[RulesPythonExecutor])

  }

}
