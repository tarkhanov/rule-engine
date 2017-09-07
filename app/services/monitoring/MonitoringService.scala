package services.monitoring

import javax.inject.Inject

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import play.api.libs.json.JsValue
import services.monitoring.MonitoringActor.{Subscribe, UnSubscribe}

class MonitoringService @Inject()(implicit system: ActorSystem, mat: Materializer) {

  private val monitoringActor = system.actorOf(MonitoringActor.props)

  private val bufferSize = 16
  private val overflowStrategy = OverflowStrategy.dropNew

  def join(): Flow[Nothing, JsValue, NotUsed] = {
    val (outActor: ActorRef, publisher) = Source
      .actorRef[JsValue](bufferSize, overflowStrategy)
      .toMat(Sink.asPublisher(false))(Keep.both).run()

    val source = Source.fromPublisher(publisher)
    val sink = Sink.actorRef(monitoringActor, UnSubscribe(outActor))

    val sinkWithSubscription = Sink.fromSubscriber(
      Source.asSubscriber[Any]
      .merge(Source.single(Subscribe(outActor)))
      .toMat(sink)(Keep.left).run()
    )

    Flow.fromSinkAndSource(sinkWithSubscription, source)
  }
}