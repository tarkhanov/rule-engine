package services.execution

import models.repository.rules.RulesModel.Rule
import models.repository.types.TypeRepositoryRec
import models.repository.types.TypesModel.{Field, Type}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{MustMatchers, WordSpec}
import services.execution.RulesPythonExecutor.{BodyResult, ConditionResult}
import services.types.TypeDefinitionService

import scala.collection.mutable
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class RulesPythonExecutorSpec extends WordSpec with MustMatchers with MockitoSugar  {

  "RulesPythonExecutor" should {
    "accept int, string and lists" in {
      val mockTypeDefinitionService = mock[TypeDefinitionService]
      when(mockTypeDefinitionService.newTypeCache) thenReturn mutable.Map[String, (TypeRepositoryRec, Type)]()

      val executor = new RulesPythonExecutor(mockTypeDefinitionService)

      val requestData = Map(
        "argument1" -> List(Left("OK")),
        "argument2" -> List(Left("123")),
        "argument3s" -> List(
          Right(Map(
            "argument3s" -> List(
              Right(Map(
                "argument3" -> List(Left("?"))
              )),
              Right(Map(
                "argument3" -> List(Left("?"), Left("?"), Left("?"))
              ))
            )
          ))
        )
      )

      val definition =
        """<RuleSet seq="CMP0" name="BasicRuleSet">
          |  <arguments>
          |    <argument name="argument1" type="string"/><argument name="argument2" type="int"/><argument name="argument3" type="list[list[string]]"/>
          |  </arguments>
          |  <results>
          |    <result name="result1" type="string"/>
          |  </results>
          |  <rules>
          |    <rule name="Example">
          |      <condition>argument1 == &quot;OK&quot;</condition>
          |      <body>result1 = &quot;Success&quot;</body>
          |    </rule>
          |  </rules>
          |</RuleSet>
        """.stripMargin

      val result = Await.result(executor.execute(requestData, 123, definition), 5.seconds)

      result.length mustBe 1
      val (rule, condition, Some(body)) = result.head
      rule.name mustBe Some("Example")
      condition.value mustBe true
      condition.exceptions.isEmpty mustBe true
      body.value.length mustBe 1
      val (ruleName, ruleValue) = body.value.head
      ruleName mustBe "result1"
      ruleValue.head mustBe Left("Success")
      body.exceptions.isEmpty mustBe true
    }

    "work with multiple rules" in {
      val mockTypeDefinitionService = mock[TypeDefinitionService]
      when(mockTypeDefinitionService.newTypeCache) thenReturn mutable.Map[String, (TypeRepositoryRec, Type)]()

      val executor = new RulesPythonExecutor(mockTypeDefinitionService)

      val requestData = Map(
        "argument1" -> List(Left("OK")),
        "argument2" -> List(Left("123")),
        "argument3s" -> List(
          Right(Map(
            "argument3s" -> List(
              Right(Map(
                "argument3" -> List(Left("?"))
              )),
              Right(Map(
                "argument3" -> List(Left("?"), Left("?"), Left("?"))
              ))
            )
          ))
        )
      )

      val definition =
        """<RuleSet seq="CMP0" name="BasicRuleSet">
          |  <arguments>
          |    <argument name="argument1" type="string"/><argument name="argument2" type="int"/><argument name="argument3" type="list[list[string]]"/>
          |  </arguments>
          |  <results>
          |    <result name="result1" type="string"/>
          |  </results>
          |  <rules>
          |    <rule name="Example1">
          |      <condition>argument1 == &quot;OK&quot;</condition>
          |      <body>result1 = &quot;Success1&quot;</body>
          |    </rule>
          |    <rule name="Example2">
          |      <condition>argument2 == 123</condition>
          |      <body>result1 = &quot;Success2&quot;</body>
          |    </rule>
          |    <rule name="Example3">
          |      <condition>argument3[0][0] == &quot;?&quot;</condition>
          |      <body>result1 = &quot;Success3&quot;</body>
          |    </rule>
          |  </rules>
          |</RuleSet>
        """.stripMargin

      val result = Await.result(executor.execute(requestData, 123, definition), 5.seconds)

      result.length mustBe 3

      def checkResult(index: Int) {
        val (rule, condition, Some(body)) = result(index)
        rule.name mustBe Some(s"Example${index + 1}")
        condition.value mustBe true
        condition.exceptions.isEmpty mustBe true
        body.value.length mustBe 1
        val (ruleName, ruleValue) = body.value.head
        ruleName mustBe "result1"
        ruleValue.head mustBe Left(s"Success${index + 1}")
        body.exceptions.isEmpty mustBe true
      }

      checkResult(0)
      checkResult(1)
      checkResult(2)
    }

    "accept structures" in {
      val mockTypeDefinitionService = mock[TypeDefinitionService]
      when(mockTypeDefinitionService.newTypeCache) thenReturn mutable.Map[String, (TypeRepositoryRec, Type)]()
      when(mockTypeDefinitionService.typeDefinitionLookup(Matchers.eq("sequence:CMYe"), Matchers.any())) thenReturn
        Future.successful(Some(Type(seq = Some("sequence:CMYe"), name = "Type1", fields = List(Field("field1", "string"), Field("field2", "int")))))

      val executor = new RulesPythonExecutor(mockTypeDefinitionService)

      val requestData = Map(
        "argument1" -> List(
          Right(Map(
            "field1" -> List(Left("OK")),
            "field2" -> List(Left("2"))
          ))
        )
      )

      val definition =
        """<RuleSet seq="CMe3" name="MyRuleSet">
          |  <arguments>
          |    <argument name="argument1" type="sequence:CMYe"/>
          |  </arguments>
          |  <results>
          |    <result name="result1" type="string"/>
          |  </results>
          |  <rules>
          |    <rule name="Example">
          |      <condition>argument1.field1 == &quot;OK&quot;</condition>
          |      <body>result1 = &quot;Success&quot;</body>
          |    </rule>
          |  </rules>
          |</RuleSet>
        """.stripMargin

      val result: Seq[(Rule, ConditionResult, Option[BodyResult])] = Await.result(executor.execute(requestData, 123, definition), 5.seconds)

      result.length mustBe 1
      val (rule, condition, Some(body)) = result.head
      rule.name mustBe Some("Example")
      condition.value mustBe true
      condition.exceptions.isEmpty mustBe true
      body.value.length mustBe 1
      val (ruleName, ruleValue) = body.value.head
      ruleName mustBe "result1"
      ruleValue.head mustBe Left("Success")
      body.exceptions.isEmpty mustBe true
    }
  }

  // TODO: Test error handling

}