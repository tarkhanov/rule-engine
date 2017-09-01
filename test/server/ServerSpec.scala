package server

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status
import play.api.test.{Helpers, WsTestClient}

/**
  * Runs a play server on the default test port (Helpers.testServerPort == 19001).
  */
class ServerSpec extends PlaySpec with GuiceOneServerPerSuite with ScalaFutures with IntegrationPatience {

  private implicit val implicitPort: Int = port

  "Server" should {

    "redirect to https from root page" in {
      whenReady(WsTestClient.wsUrl("/").get) { response =>
        response.status mustBe Status.PERMANENT_REDIRECT
      }
    }

    "redirect to https from login page" in {
      whenReady(WsTestClient.wsUrl("/login").get) { response =>
        response.status mustBe Status.PERMANENT_REDIRECT
      }
    }

  }
}
