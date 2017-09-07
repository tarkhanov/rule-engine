package modules

import com.google.inject.AbstractModule
import persistence.DBUtil

class GeneralModule extends AbstractModule {

  override def configure(): Unit = {

    bind(classOf[DBUtil])

  }

}
