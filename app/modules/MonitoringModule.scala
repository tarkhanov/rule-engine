package modules

import com.google.inject.AbstractModule
import services.monitoring.{MonitoringService, MonitoringServiceImpl}

class MonitoringModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[MonitoringService]).to(classOf[MonitoringServiceImpl]).asEagerSingleton()
  }

}
