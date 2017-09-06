package controllers

import javax.inject.Inject

import akka.stream.scaladsl.Flow
import com.typesafe.scalalogging.StrictLogging
import controllers.security.AuthAction
import play.api.http.websocket._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.streams.AkkaStreams
import play.api.mvc.WebSocket
import play.api.mvc.WebSocket.MessageFlowTransformer
import services.monitoring.MonitoringService
import controllers.MonitoringController.jsonResponseMessageFlowTransformer

import scala.concurrent.Future

object MonitoringController {

  implicit val jsonResponseMessageFlowTransformer: MessageFlowTransformer[Nothing, JsValue] = {
    (flow: Flow[Nothing, JsValue, _]) => {
      AkkaStreams.bypassWith[Message, Nothing, Message](Flow[Message].collect {
        case _ => Right(CloseMessage(Some(CloseCodes.Unacceptable), "This WebSocket only supports server to client messages"))
      })(flow map { json => TextMessage(Json.stringify(json)) })
    }
  }

}

class MonitoringController @Inject()(AuthenticatedAction: AuthAction,
                                     monitoringService: MonitoringService)
                                     extends InternationalInjectedController with StrictLogging {

  def monitoring = AuthenticatedAction {
    implicit request =>
      Ok(views.html.monitoring.monitoring(request.user, request.error, request.log))
  }

  def wsMonitoring: WebSocket = WebSocket.acceptOrResult[Nothing, JsValue] { request =>
    Future.successful(request.session.get("user") match {
      case None => Left(Forbidden)
      case Some(_) => Right(monitoringService.join())
    })
  }

}
