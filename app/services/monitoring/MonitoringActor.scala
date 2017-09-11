package services.monitoring

import java.lang.management.ManagementFactory
import java.util.Date
import javax.management.{Attribute, ObjectName}

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
import play.api.libs.json.{JsObject, JsString, Json}
import services.monitoring.MonitoringActor.{Subscribe, UnSubscribe, UpdateStatusInfo}

import scala.concurrent.duration._
import scala.language.postfixOps

object MonitoringActor {

  def props: Props = Props(new MonitoringActor)

  case class Subscribe(actor: ActorRef)

  case class UnSubscribe(actor: ActorRef)

  case class UpdateStatusInfo(status: JsObject)

  def collectStatusInfo(): JsObject = {

    def getProcessCpuLoad: (Int, Int) = {

      val mbs = ManagementFactory.getPlatformMBeanServer
      val name = ObjectName.getInstance("java.lang:type=OperatingSystem")
      val list = mbs.getAttributes(name, List("SystemCpuLoad", "ProcessCpuLoad").toArray)

      val att = list.get(0).asInstanceOf[Attribute]
      val systemCPULoad = att.getValue.asInstanceOf[Double]
      val att1 = list.get(1).asInstanceOf[Attribute]
      val processCPULoad = att1.getValue.asInstanceOf[Double]

      def toPercentage(value: Double): Int = {
        val res = if (value == -1.0) Float.NaN
        else ((value * 1000) / 10.0f).toFloat
        math.round(math.floor(res)).toInt
      }

      (toPercentage(systemCPULoad), toPercentage(processCPULoad))
    }

    val runtime = Runtime.getRuntime
    val mb = 1024 * 1024
    val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / mb
    val totalMemory = runtime.totalMemory() / mb

    val (systemLoad, processLoad) = getProcessCpuLoad

    Json.obj(
      "ts" -> JsString(new Date().getTime.toString),
      "sl" -> JsString(systemLoad.toString),
      "pl" -> JsString(processLoad.toString),
      "um" -> JsString(usedMemory.toString),
      "tm" -> JsString(totalMemory.toString)
    )
  }
}

class MonitoringActor extends Actor with ActorLogging {

  import context.dispatcher

  private var subscribers = Set.empty[ActorRef]

  private def start(): Cancellable = {
    log.debug("Start Monitoring")
    context.system.scheduler.schedule(0 seconds, 1 second) {
      self ! UpdateStatusInfo(MonitoringActor.collectStatusInfo())
    }
  }

  private def stop(job: Cancellable): Unit = {
    log.debug("Stop Monitoring")
    job.cancel()
  }

  def receive: Receive = receiverWhileJobIsStopped()

  def receiverWhileJobIsRunning(job: Cancellable): Receive = {
    case Subscribe(actor) =>
      subscribers += actor

    case UnSubscribe(actor) =>
      subscribers -= actor
      if (subscribers.isEmpty) {
        stop(job)
        context.become(receiverWhileJobIsStopped())
      }
    case UpdateStatusInfo(status) =>
      subscribers.foreach(_ ! status)
  }

  def receiverWhileJobIsStopped(): Receive = {
    case Subscribe(actor) =>
      subscribers += actor
      context.become(receiverWhileJobIsRunning(start()))
    case _: UnSubscribe =>
  }

}


