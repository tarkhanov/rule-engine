package controllers

import javax.inject.Inject

import akka.actor.{Actor, ActorRef, Props}
import com.typesafe.scalalogging.StrictLogging
import controllers.MonitoringController.MonitoringWSActor
import controllers.security.AuthenticatedAction
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsValue
import play.api.mvc.{Controller, WebSocket}
import services.monitoring.MonitoringService

/**
 * Created by Sergey Tarkhanov on 8/14/2015.
 */

object MonitoringController {

  class MonitoringWSActor(out: ActorRef, monitoringService: MonitoringService) extends Actor {

    override def preStart(): Unit =
      monitoringService.subscribe(self)

    override def postStop(): Unit =
      monitoringService.unsubscribe(self)

    def receive: Receive = {
      case value: JsValue =>
        out ! value
    }

  }

}

class MonitoringController @Inject()(val messagesApi: MessagesApi, monitoringService: MonitoringService) extends Controller with I18nSupport with RequestImplicits with StrictLogging {

  def monitoring = AuthenticatedAction {
    implicit request =>
      Ok(views.html.monitoring.monitoring(request.user, request.error, request.log))
  }

  import play.api.Play.current

  def wsMonitoring: WebSocket[String, JsValue] = WebSocket.acceptWithActor[String, JsValue] {
    _ =>
      (out: ActorRef) =>
        logger.debug("Creating actor for web socket")
        Props(new MonitoringWSActor(out, monitoringService))
  }

}
