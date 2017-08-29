package services.monitoring

import javax.inject.{Inject, Singleton}

import akka.actor.{ActorRef, ActorSystem, Props}
import services.monitoring.MonitoringActor.{Subscribe, UnSubscribe}

@Singleton
class MonitoringService @Inject()(system: ActorSystem) {

  private val monitoringActor = system.actorOf(Props(new MonitoringActor()))

  def subscribe(actor: ActorRef): Unit = monitoringActor ! Subscribe(actor)

  def unsubscribe(actor: ActorRef): Unit = monitoringActor ! UnSubscribe(actor)

}