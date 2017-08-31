package services.monitoring

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest._
import play.api.libs.json.JsObject

import scala.concurrent.duration._

class MonitoringActorSpec extends TestKit(ActorSystem("MonitoringActorSpec"))
  with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll  {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "Monitoring Actor" should {

    "push status to subscriber" in {

      val actor = system.actorOf(MonitoringActor.props)
      within(200.millis) {
        actor ! MonitoringActor.Subscribe(self)
        val status = expectMsgClass(classOf[JsObject])
        (status \ "ts").isDefined shouldBe true
        (status \ "sl").isDefined shouldBe true
        (status \ "pl").isDefined shouldBe true
        (status \ "um").isDefined shouldBe true
        (status \ "tm").isDefined shouldBe true
      }

      expectMsgClass(1.5.seconds, classOf[JsObject])
      expectNoMsg(0.5.seconds)
      expectMsgClass(1.seconds, classOf[JsObject])
      expectNoMsg(0.5.seconds)
      expectMsgClass(1.seconds, classOf[JsObject])

      actor ! MonitoringActor.UnSubscribe(self)
      expectNoMsg(2.seconds)
    }

  }

}
