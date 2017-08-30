package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.scalalogging.StrictLogging
import controllers.security.AuthAction
import play.api.libs.json.JsValue
import play.api.mvc.WebSocket
import services.monitoring.MonitoringService

import scala.concurrent.Future

object MonitoringController {

//  object MonitoringWSActor {
//
//    def props(out: ActorRef, monitoringService: MonitoringService) = Props(new MonitoringWSActor(out, monitoringService))
//
//  }
//
//  class MonitoringWSActor(out: ActorRef, monitoringService: MonitoringService) extends Actor {
//
//    override def preStart(): Unit =
//      monitoringService.subscribe(self)
//
//    override def postStop(): Unit =
//      monitoringService.unsubscribe(self)
//
//    def receive: Receive = {
//      case value: JsValue =>
//        out ! value
//    }
//
//  }

}

class MonitoringController @Inject()(AuthenticatedAction: AuthAction, monitoringService: MonitoringService)
                                     extends InternationalInjectedController with StrictLogging {

  def monitoring = AuthenticatedAction {
    implicit request =>
      Ok(views.html.monitoring.monitoring(request.user, request.error, request.log))
  }

  //  def wsMonitoring: WebSocket = WebSocket.acceptOrResult[JsValue, JsValue] { request =>
  //    Future.successful(request.session.get("user") match {
  //      case None => Left(Forbidden)
  //      case Some(_) => Right(ActorFlow.actorRef { out => MonitoringWSActor.props(out, monitoringService) })
  //    })
  //  }

  def wsMonitoring = WebSocket.acceptOrResult[JsValue, JsValue] { request =>
    Future.successful(request.session.get("user") match {
      case None => Left(Forbidden)
      case Some(_) => Right(monitoringService.join())
    })
  }



}
