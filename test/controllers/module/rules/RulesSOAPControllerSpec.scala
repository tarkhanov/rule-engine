package controllers.module.rules

import java.io.StringReader

import akka.stream.Materializer
import akka.util.ByteString
import models.repository.rules.RulesModel.{Body, Condition, Rule}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import services.execution.RulesPythonExecutor.{BodyResult, ConditionResult, ExecutionResult}
import services.rules.RulesService

import scala.concurrent.Future

class RulesSOAPControllerSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar with Results   {

  implicit val materializer: Materializer = app.materializer

  val requestData = Map(
    "argument1" -> List(Left("OK")),
    "argument2s" -> List(Right(Map("argument2s" -> List(Right(Map("argument2" -> List(Left("?")))))))),
    "argument2" -> List(Left("123"))
  )

  val responseData: List[(Rule, ConditionResult, Option[BodyResult])] = List(
      (Rule(Some("Example"), Condition("argument1 == \"OK\""), Body("result1 = \"Success\"")),
      ExecutionResult(true),
      Some(ExecutionResult(List(("result1", List(Left("Success"))))))
    ))

  "RulesSOAPController" should {
    "accept int, string and lists" in {

      val filter = "mockfilter"

      val mockRulesService = mock[RulesService]
      when(mockRulesService.invoke(Matchers.eq(requestData), Matchers.eq(filter))) thenReturn Future.successful(responseData)

      val controller = new RulesSOAPController(mockRulesService)
      controller.setControllerComponents(Helpers.stubControllerComponents(playBodyParsers = stubPlayBodyParsers(materializer)))

      val requestBody = ByteString(
        """<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:rul="http://example.com/rules/">
          |   <soapenv:Header/>
          |   <soapenv:Body>
          |      <rul:rulesRequest>
          |         <argument1>OK</argument1>
          |         <argument2>123</argument2>
          |         <argument2s>
          |            <!--Zero or more repetitions:-->
          |            <argument2s>
          |               <!--Zero or more repetitions:-->
          |               <argument2>?</argument2>
          |            </argument2s>
          |         </argument2s>
          |      </rul:rulesRequest>
          |   </soapenv:Body>
          |</soapenv:Envelope>
        """.stripMargin)

      val request = FakeRequest("POST", s"/interface/rules/$filter/soap")
        .withHeaders("Accept-Encoding" -> "gzip,deflate",
        "Content-Type" -> "text/xml;charset=UTF-8",
        "SOAPAction" -> "\"http://example.com/rules/invoke\"",
        "Content-Length" -> requestBody.length.toString,
        "Host" -> "localhost",
        "Connection" -> "Keep-Alive",
        "User-Agent" -> "Apache-HttpClient/4.1.1")

      val result = controller.invoke(filter).apply(request).run(requestBody)

      status(result) mustBe 200

      val resultText = contentAsString(result)
      val resultXml = scala.xml.XML.load(new StringReader(resultText))

      (resultXml \\ "rule").length mustBe 1
      (resultXml \\ "rule" \ "result1").length mustBe 1
      (resultXml \\ "rule" \ "result1").head.text.trim  mustBe "Success"
    }
  }
}
