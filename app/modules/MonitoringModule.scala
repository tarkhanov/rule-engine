package modules

import com.google.inject.AbstractModule
import services.monitoring.MonitoringService

class MonitoringModule extends AbstractModule {

  override def configure(): Unit = {

    bind(classOf[MonitoringService]).asEagerSingleton()

  }

}
