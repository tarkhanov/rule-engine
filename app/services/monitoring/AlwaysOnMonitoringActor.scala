package services.monitoring

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
import play.api.libs.json.JsObject
import services.monitoring.AlwaysOnMonitoringActor.{Subscribe, UnSubscribe, UpdateStatusInfo}

import scala.concurrent.duration._
import scala.language.postfixOps

object AlwaysOnMonitoringActor {

  def props: Props = Props(new AlwaysOnMonitoringActor)

  case class Subscribe(actor: ActorRef)

  case class UnSubscribe(actor: ActorRef)

  case class UpdateStatusInfo(status: JsObject)

}

class AlwaysOnMonitoringActor extends Actor with ActorLogging {

  import context.dispatcher

  private var subscribers = Set.empty[ActorRef]

  private var job: Option[Cancellable] = None

  override def preStart(): Unit = {
    log.debug("Start Monitoring")
    job = Some(context.system.scheduler.schedule(0 seconds, 1 second) {
      self ! UpdateStatusInfo(MonitoringUtil.collectStatusInfo())
    })
  }

  override def postStop(): Unit = {
    job.foreach { c =>
      log.debug("Stop Monitoring")
      c.cancel()
    }
  }

  private val historyLength = 120
  private var updateHistory = Vector.empty[JsObject]

  def receive: Receive = {
    case Subscribe(actor) =>
      subscribers += actor
      updateHistory.foreach(actor ! _)

    case UnSubscribe(actor) =>
      subscribers -= actor

    case UpdateStatusInfo(status) =>
      updateHistory :+= status
      if (updateHistory.length > historyLength) {
        updateHistory = updateHistory.takeRight(historyLength)
      }
      subscribers.foreach(_ ! status)
  }

}


