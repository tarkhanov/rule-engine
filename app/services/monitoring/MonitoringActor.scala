package services.monitoring

import java.lang.management.ManagementFactory
import java.util.Date
import javax.management.{Attribute, ObjectName}

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable}
import akka.event.{EventBus, LookupClassification}
import play.api.libs.json.{JsObject, JsString, JsValue}
import services.monitoring.MonitoringActor.{Subscribe, UnSubscribe}

import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Created by Sergey Tarkhanov on 8/15/2015.
 */

object MonitoringActor {

  final case class Subscribe(actor: ActorRef)

  final case class UnSubscribe(actor: ActorRef)
}

class MonitoringActor extends Actor with ActorLogging with EventBus with LookupClassification {

  import context._

  type Event = JsValue
  type Classifier = String
  type Subscriber = ActorRef

  override protected def classify(event: Event): Classifier = ""

  override protected def publish(event: Event, subscriber: Subscriber): Unit = subscriber ! event

  override protected def compareSubscribers(a: Subscriber, b: Subscriber): Int = a.compareTo(b)

  override protected def mapSize(): Int = 128

  private def start(): Cancellable = {

    log.debug("Start Monitoring")

    context.system.scheduler.schedule(0 seconds, 1 second) {

      def getProcessCpuLoad: (Int, Int) = {

        val mbs = ManagementFactory.getPlatformMBeanServer
        val name = ObjectName.getInstance("java.lang:type=OperatingSystem")
        val list = mbs.getAttributes(name, List("SystemCpuLoad", "ProcessCpuLoad").toArray)

        val att = list.get(0).asInstanceOf[Attribute]
        val systemCPULoad = att.getValue.asInstanceOf[Double]
        val att1 = list.get(1).asInstanceOf[Attribute]
        val processCPULoad = att1.getValue.asInstanceOf[Double]

        def toPercentage(value: Double): Int = {
          val res = if (value == -1.0) Float.NaN // usually takes a couple of seconds before we get real values
          else ((value * 1000) / 10.0f).toFloat // returns a percentage value with 1 decimal point precision
          math.round(math.floor(res)).toInt
        }

        (toPercentage(systemCPULoad), toPercentage(processCPULoad))
      }

      val runtime = Runtime.getRuntime
      val mb = 1024 * 1024
      val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / mb
      val totalMemory = runtime.totalMemory() / mb

      val (systemLoad, processLoad) = getProcessCpuLoad

      val status = mutable.MutableList[(String, JsValue)]()
      status += "ts" -> JsString(new Date().getTime.toString)
      status += "sl" -> JsString(systemLoad.toString)
      status += "pl" -> JsString(processLoad.toString)
      status += "um" -> JsString(usedMemory.toString)
      status += "tm" -> JsString(totalMemory.toString)

      publish(JsObject(status))
    }
  }

  private def stop(job: Cancellable): Unit = {
    log.debug("Stop Monitoring")
    job.cancel()
  }

  def receive: Receive = receiverWhileJobIsStopped()

  def receiverWhileJobIsRunning(job: Cancellable): Receive = {
    case Subscribe(actor: ActorRef) =>
      subscribe(actor, "")
    case UnSubscribe(actor: ActorRef) =>
      unsubscribe(actor)
      if (subscribers.isEmpty) {
        stop(job)
        become(receiverWhileJobIsStopped())
      }
  }

  def receiverWhileJobIsStopped(): Receive = {
    case Subscribe(actor: ActorRef) =>
      subscribe(actor, "")
      become(receiverWhileJobIsRunning(start()))
    case UnSubscribe(actor: ActorRef) =>
      Unit
  }
}


