package services.monitoring

import javax.inject.{Inject, Singleton}

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import play.api.libs.json.JsValue
import services.monitoring.MonitoringActor.{Subscribe, UnSubscribe}

@Singleton
class MonitoringService @Inject()(implicit system: ActorSystem, mat: Materializer) {

  private val monitoringActor = system.actorOf(MonitoringActor.props)

  private val bufferSize = 256
  private val overflowStrategy: OverflowStrategy = OverflowStrategy.dropNew

  def join(): Flow[JsValue, JsValue, NotUsed] = {
    val (outActor: ActorRef, publisher) = Source
      .actorRef[JsValue](bufferSize, overflowStrategy)
      .toMat(Sink.asPublisher(false))(Keep.both).run()

    val source = Source.fromPublisher(publisher)
    val sink = Sink.actorRef[AnyRef](monitoringActor, UnSubscribe(outActor))

    val sinkWithFilter = Sink.fromSubscriber(
      Source.asSubscriber[JsValue]
      .filter(_ => false)
      .merge(Source.single(Subscribe(outActor)))
      .toMat(sink)(Keep.left).run()
    )

    Flow.fromSinkAndSource(sinkWithFilter, source)
  }
}