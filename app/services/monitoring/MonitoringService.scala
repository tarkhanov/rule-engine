package services.monitoring

import javax.inject.{Inject, Singleton}

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import play.api.libs.json.JsValue
import services.monitoring.MonitoringActor.{Subscribe, UnSubscribe}

@Singleton
class MonitoringService @Inject()(implicit system: ActorSystem, mat: Materializer) {

  private val monitoringActor = system.actorOf(MonitoringActor.props)

  def subscribe(actor: ActorRef): Unit = monitoringActor ! Subscribe(actor)

  def unsubscribe(actor: ActorRef): Unit = monitoringActor ! UnSubscribe(actor)

  private val bufferSize = 256
  private val overflowStrategy: OverflowStrategy = OverflowStrategy.dropNew

  def join(): Flow[JsValue, JsValue, NotUsed] = {
    val (outActor: ActorRef, publisher) = Source
      .actorRef[JsValue](bufferSize, overflowStrategy)
      .toMat(Sink.asPublisher(false))(Keep.both).run()

    val source = Source.fromPublisher(publisher).filter(_ => true) // Skip input messages
    val sink = Sink.actorRef[JsValue](monitoringActor, UnSubscribe(outActor))
    // .contramap { js: JsValue => js } // transform input messages

    subscribe(outActor)

    Flow.fromSinkAndSource(sink, source)
  }
}