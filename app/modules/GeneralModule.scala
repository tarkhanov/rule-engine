package modules

import com.google.inject.AbstractModule
import controllers.ErrorHandler
import persistence.DBUtil

class GeneralModule extends AbstractModule {

  override def configure(): Unit = {

    bind(classOf[ErrorHandler])
    bind(classOf[DBUtil])

  }

}
