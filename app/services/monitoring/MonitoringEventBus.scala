package services.monitoring

import akka.actor.ActorRef
import akka.event.{EventBus, LookupClassification}
import play.api.libs.json.JsValue

class MonitoringEventBus extends EventBus with LookupClassification {

  type Event = JsValue
  type Classifier = String
  type Subscriber = ActorRef

  override protected def classify(event: Event): Classifier = ""

  override protected def publish(event: Event, subscriber: Subscriber): Unit = subscriber ! event

  override protected def compareSubscribers(a: Subscriber, b: Subscriber): Int = a.compareTo(b)

  override protected def mapSize(): Int = 128

  def isEmpty: Boolean = subscribers.isEmpty

}
