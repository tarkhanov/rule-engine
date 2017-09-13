package services.monitoring

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
import play.api.libs.json.JsObject
import services.monitoring.MonitoringActor.{Subscribe, UnSubscribe, UpdateStatusInfo}

import scala.concurrent.duration._
import scala.language.postfixOps

@Deprecated
object MonitoringActor {

  def props: Props = Props(new MonitoringActor)

  case class Subscribe(actor: ActorRef)

  case class UnSubscribe(actor: ActorRef)

  case class UpdateStatusInfo(status: JsObject)

}

@Deprecated
class MonitoringActor extends Actor with ActorLogging {

  import context.dispatcher

  private var subscribers = Set.empty[ActorRef]

  private def start(): Unit = {
    stop()
    log.debug("Start Monitoring")
    job = Some(context.system.scheduler.schedule(0 seconds, 1 second) {
      self ! UpdateStatusInfo(MonitoringUtil.collectStatusInfo())
    })
  }

  private def stop(): Unit = {
    job.foreach { c =>
      log.debug("Stop Monitoring")
      c.cancel()
    }
  }

  private var job: Option[Cancellable] = None

  override def postStop(): Unit = stop()

  def receive: Receive = receiverWhileJobIsStopped

  def receiverWhileJobIsRunning: Receive = {
    case Subscribe(actor) =>
      subscribers += actor

    case UnSubscribe(actor) =>
      subscribers -= actor
      if (subscribers.isEmpty) {
        stop()
        context.become(receiverWhileJobIsStopped)
      }
    case UpdateStatusInfo(status) =>
      subscribers.foreach(_ ! status)
  }

  def receiverWhileJobIsStopped: Receive = {
    case Subscribe(actor) =>
      subscribers += actor
      start()
      context.become(receiverWhileJobIsRunning)
    case _: UnSubscribe =>
  }

}


