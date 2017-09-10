package server

import com.typesafe.config.ConfigFactory
import org.openqa.selenium.WebDriver
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.{Application, Configuration}
import play.api.inject.guice.GuiceApplicationBuilder

/**
  * Runs a browser test using Fluentium against a play application on a server port.
  */
class BrowserSpec extends PlaySpec
  with OneBrowserPerSuite
  with GuiceOneServerPerSuite
  with HtmlUnitFactory
  with ServerProvider {

  override def createWebDriver(): WebDriver = HtmlUnitFactory.createWebDriver(false)

  private val conf: Configuration = Configuration(ConfigFactory.parseString(
    """play.filters.https.redirectEnabled = false
    """.stripMargin))

  override def fakeApplication(): Application =
    // Disable redirect to https
    new GuiceApplicationBuilder().configure(conf).build()

  "Application" should {

    "be able to login and open some pages" in {

      go to ("http://localhost:" + port + "/login")

      pageTitle mustBe "Login - Rule Engine"

      textField(name("login")).value = "admin"
      pwdField(name("password")).value = "password"
      click on find(name("login-button")).value
      eventually {
        pageTitle mustBe "Components - Rule Engine"
      }

      go to ("http://localhost:" + port + "/private/settings")

      pageTitle mustBe "Settings - Rule Engine"
      pageSource must include ("List of Users")

      go to ("http://localhost:" + port + "/private/monitoring")

      pageTitle mustBe "Monitoring - Rule Engine"
      pageSource must include ("CPU")
      pageSource must include ("Memory")

    }

  }
}
