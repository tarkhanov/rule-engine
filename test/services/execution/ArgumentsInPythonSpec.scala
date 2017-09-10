package services.execution

import models.repository.rules.RulesModel.Argument
import models.repository.types.TypesModel.{Field, Type}
import org.mockito.Matchers
import org.mockito.Mockito.when
import org.python.core.PyBoolean
import org.python.util.PythonInterpreter
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{MustMatchers, WordSpec}
import services.types.TypeDefinitionService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class ArgumentsInPythonSpec extends WordSpec with MustMatchers with MockitoSugar  {


  "ArgumentsInPython" should {

    "define int parameter" in {
      val mockTypeDefinitionService = mock[TypeDefinitionService]
      val typeCache = mockTypeDefinitionService.newTypeCache

      val arguments = List(Argument("parameter1", "int"))
      val requestData = Map("parameter1" -> Seq(Left("123")))
      val codeFuture = ArgumentsInPython.generateArguments(arguments, requestData, mockTypeDefinitionService, typeCache)
      val code = Await.result(codeFuture, 5.seconds)

      utils.using(new PythonInterpreter()) { pi =>
        pi.exec(code)
        pi.eval("parameter1 == 123") mustBe new PyBoolean(true)
      }
    }

    "define int parameter without data" in {
      val mockTypeDefinitionService = mock[TypeDefinitionService]
      val typeCache = mockTypeDefinitionService.newTypeCache

      val arguments = List(Argument("parameter1", "int"))
      val requestData = Map("wrong parameter name" -> Seq(Left("123")))
      val codeFuture = ArgumentsInPython.generateArguments(arguments, requestData, mockTypeDefinitionService, typeCache)
      val code = Await.result(codeFuture, 5.seconds)

      utils.using(new PythonInterpreter()) { pi =>
        pi.exec(code)
        pi.eval("parameter1 is None") mustBe new PyBoolean(true)
      }
    }


    "define string parameter" in {
      val mockTypeDefinitionService = mock[TypeDefinitionService]
      val typeCache = mockTypeDefinitionService.newTypeCache

      val arguments = List(Argument("parameter1", "string"))
      val requestData = Map("parameter1" -> Seq(Left("123")))
      val codeFuture = ArgumentsInPython.generateArguments(arguments, requestData, mockTypeDefinitionService, typeCache)
      val code = Await.result(codeFuture, 5.seconds)

      utils.using(new PythonInterpreter()) { pi =>
        pi.exec(code)
        pi.eval("parameter1 == \"123\"") mustBe new PyBoolean(true)
      }
    }

    "define string parameter without data" in {
      val mockTypeDefinitionService = mock[TypeDefinitionService]
      val typeCache = mockTypeDefinitionService.newTypeCache

      val arguments = List(Argument("parameter1", "string"))
      val requestData = Map("wrong parameter name" -> Seq(Left("123")))
      val codeFuture = ArgumentsInPython.generateArguments(arguments, requestData, mockTypeDefinitionService, typeCache)
      val code = Await.result(codeFuture, 5.seconds)

      utils.using(new PythonInterpreter()) { pi =>
        pi.exec(code)
        pi.eval("parameter1 is None") mustBe new PyBoolean(true)
      }
    }

    "define string parameter with escape sequences" in {
      val mockTypeDefinitionService = mock[TypeDefinitionService]
      val typeCache = mockTypeDefinitionService.newTypeCache

      val arguments = List(Argument("parameter1", "string"))
      val requestData = Map("parameter1" -> Seq(Left("12\"345\"\"\"6789")))
      val codeFuture = ArgumentsInPython.generateArguments(arguments, requestData, mockTypeDefinitionService, typeCache)
      val code = Await.result(codeFuture, 5.seconds)

      utils.using(new PythonInterpreter()) { pi =>
        pi.exec(code)
        pi.eval("parameter1 == '12\"345\"\"\"6789'") mustBe new PyBoolean(true)
      }
    }

    "define list of ints parameter" in {
      val mockTypeDefinitionService = mock[TypeDefinitionService]
      val typeCache = mockTypeDefinitionService.newTypeCache

      val arguments = List(Argument("parameter1", "list[int]"))
      val requestData = Map("parameter1s" -> Seq(Right(Map("parameter1" -> Seq(Left("123"), Left("456"), Left("789"))))))
      val codeFuture = ArgumentsInPython.generateArguments(arguments, requestData, mockTypeDefinitionService, typeCache)
      val code = Await.result(codeFuture, 5.seconds)

      utils.using(new PythonInterpreter()) { pi =>
        pi.exec(code)
        pi.eval("len(parameter1) == 3") mustBe new PyBoolean(true)
        pi.eval("parameter1[0] == 123") mustBe new PyBoolean(true)
        pi.eval("parameter1[1] == 456") mustBe new PyBoolean(true)
        pi.eval("parameter1[2] == 789") mustBe new PyBoolean(true)
      }
    }

    "define list of ints parameter without data" in {
      val mockTypeDefinitionService = mock[TypeDefinitionService]
      val typeCache = mockTypeDefinitionService.newTypeCache

      val arguments = List(Argument("parameter1", "list[int]"))
      val requestData = Map("wrong parameter name" -> Seq(Right(Map("parameter1" -> Seq(Left("123"), Left("456"), Left("789"))))))
      val codeFuture = ArgumentsInPython.generateArguments(arguments, requestData, mockTypeDefinitionService, typeCache)
      val code = Await.result(codeFuture, 5.seconds)

      utils.using(new PythonInterpreter()) { pi =>
        pi.exec(code)
        pi.eval("parameter1 is None") mustBe new PyBoolean(true)
      }
    }

    "define list of ints parameter without data for items" in {
      val mockTypeDefinitionService = mock[TypeDefinitionService]
      val typeCache = mockTypeDefinitionService.newTypeCache

      val arguments = List(Argument("parameter1", "list[int]"))
      val requestData = Map("parameter1s" -> Seq(Right(Map("wrong name" -> Seq(Left("123"), Left("456"), Left("789"))))))
      val codeFuture = ArgumentsInPython.generateArguments(arguments, requestData, mockTypeDefinitionService, typeCache)
      val code = Await.result(codeFuture, 5.seconds)

      utils.using(new PythonInterpreter()) { pi =>
        pi.exec(code)
        pi.eval("parameter1 == []") mustBe new PyBoolean(true)
      }
    }

    "define list of strings parameter" in {
      val mockTypeDefinitionService = mock[TypeDefinitionService]
      val typeCache = mockTypeDefinitionService.newTypeCache

      val arguments = List(Argument("parameter1", "list[string]"))
      val requestData = Map("parameter1s" -> Seq(Right(Map("parameter1" -> Seq(Left("123"), Left("456"), Left("789"))))))
      val codeFuture = ArgumentsInPython.generateArguments(arguments, requestData, mockTypeDefinitionService, typeCache)
      val code = Await.result(codeFuture, 5.seconds)

      utils.using(new PythonInterpreter()) { pi =>
        pi.exec(code)
        pi.eval("len(parameter1) == 3") mustBe new PyBoolean(true)
        pi.eval("parameter1[0] == \"123\"") mustBe new PyBoolean(true)
        pi.eval("parameter1[1] == \"456\"") mustBe new PyBoolean(true)
        pi.eval("parameter1[2] == \"789\"") mustBe new PyBoolean(true)
      }
    }

    "define list of lists of strings parameter" in {
      val mockTypeDefinitionService = mock[TypeDefinitionService]
      val typeCache = mockTypeDefinitionService.newTypeCache

      val arguments = List(Argument("parameter1", "list[list[string]]"))
      val requestData = Map("parameter1s" -> Seq(Right(Map("parameter1s" ->
        Seq(
          Right(Map("parameter1" -> Seq(Left("1-123"), Left("1-456"), Left("1-789")))),
          Right(Map("parameter1" -> Seq(Left("2-123"), Left("2-456"), Left("2-789"))))
        )
      ))))
      val codeFuture = ArgumentsInPython.generateArguments(arguments, requestData, mockTypeDefinitionService, typeCache)
      val code = Await.result(codeFuture, 5.seconds)

      utils.using(new PythonInterpreter()) { pi =>
        pi.exec(code)
        pi.eval("len(parameter1) == 2") mustBe new PyBoolean(true)
        pi.eval("parameter1[0][0] == \"1-123\"") mustBe new PyBoolean(true)
        pi.eval("parameter1[0][1] == \"1-456\"") mustBe new PyBoolean(true)
        pi.eval("parameter1[0][2] == \"1-789\"") mustBe new PyBoolean(true)
        pi.eval("parameter1[1][0] == \"2-123\"") mustBe new PyBoolean(true)
        pi.eval("parameter1[1][1] == \"2-456\"") mustBe new PyBoolean(true)
        pi.eval("parameter1[1][2] == \"2-789\"") mustBe new PyBoolean(true)
      }
    }

    "define structure with int, string and list type fields parameter" in {
      val mockTypeDefinitionService = mock[TypeDefinitionService]
      val typeCache = mockTypeDefinitionService.newTypeCache
      when(mockTypeDefinitionService.typeDefinitionLookup(Matchers.eq("sequence:CMYe"), Matchers.any())) thenReturn
        Future.successful(Some(Type(
          seq = Some("sequence:CMYe"),
          name = "Type1",
          fields = List(
            Field("field1", "string"),
            Field("field2", "int"),
            Field("field3", "list[int]")
          )
        )))

      val arguments = List(Argument("parameter1", "sequence:CMYe"))
      val requestData = Map(
        "parameter1" -> Seq(
          Right(Map(
            "field1" -> Seq(Left("OK")),
            "field2" -> Seq(Left("123")),
            "field3s" -> Seq(Right(Map("field3" -> Seq(Left("123"), Left("456"), Left("789")))))
          ))
        )
      )

      val codeFuture = ArgumentsInPython.generateArguments(arguments, requestData, mockTypeDefinitionService, typeCache)
      val code = Await.result(codeFuture, 5.seconds)

      utils.using(new PythonInterpreter()) { pi =>
        pi.exec(code)
        pi.eval("parameter1.field1 == \"OK\"") mustBe new PyBoolean(true)
        pi.eval("parameter1.field2 == 123") mustBe new PyBoolean(true)
        pi.eval("parameter1.field3[0] == 123") mustBe new PyBoolean(true)
        pi.eval("parameter1.field3[1] == 456") mustBe new PyBoolean(true)
        pi.eval("parameter1.field3[2] == 789") mustBe new PyBoolean(true)
      }
    }

    "define structure with structures as fields parameter" in {
      val mockTypeDefinitionService = mock[TypeDefinitionService]
      val typeCache = mockTypeDefinitionService.newTypeCache
      when(mockTypeDefinitionService.typeDefinitionLookup(Matchers.eq("id:structure"), Matchers.any())) thenReturn
        Future.successful(Some(Type(
          seq = Some("id:structure"),
          name = "Type2",
          fields = List(
            Field("field1", "sequence:CMYe"),
            Field("field2", "sequence:CMYe")
          )
        )))
      when(mockTypeDefinitionService.typeDefinitionLookup(Matchers.eq("sequence:CMYe"), Matchers.any())) thenReturn
        Future.successful(Some(Type(
          seq = Some("sequence:CMYe"),
          name = "Type1",
          fields = List(
            Field("field1", "string"),
            Field("field2", "int"),
            Field("field3", "list[int]")
          )
        )))

      val arguments = List(Argument("parameter1", "id:structure"))
      val requestData = Map(
        "parameter1" -> Seq(
          Right(Map(
            "field1" -> Seq(
                Right(Map(
                  "field1" -> Seq(Left("OK")),
                  "field2" -> Seq(Left("123")),
                  "field3s" -> Seq(Right(Map("field3" -> Seq(Left("123"), Left("456"), Left("789")))))
                ))
              ),
            "field2" -> Seq(
              Right(Map(
                "field1" -> Seq(Left("OK")),
                "field2" -> Seq(Left("123")),
                "field3s" -> Seq(Right(Map("field3" -> Seq(Left("123"), Left("456"), Left("789")))))
              ))
            ),
          ))
        )
      )

      val codeFuture = ArgumentsInPython.generateArguments(arguments, requestData, mockTypeDefinitionService, typeCache)
      val code = Await.result(codeFuture, 5.seconds)

      utils.using(new PythonInterpreter()) { pi =>
        pi.exec(code)
        pi.eval("parameter1.field1.field1 == \"OK\"") mustBe new PyBoolean(true)
        pi.eval("parameter1.field1.field2 == 123") mustBe new PyBoolean(true)
        pi.eval("parameter1.field1.field3[0] == 123") mustBe new PyBoolean(true)
        pi.eval("parameter1.field1.field3[1] == 456") mustBe new PyBoolean(true)
        pi.eval("parameter1.field1.field3[2] == 789") mustBe new PyBoolean(true)

        pi.eval("parameter1.field2.field1 == \"OK\"") mustBe new PyBoolean(true)
        pi.eval("parameter1.field2.field2 == 123") mustBe new PyBoolean(true)
        pi.eval("parameter1.field2.field3[0] == 123") mustBe new PyBoolean(true)
        pi.eval("parameter1.field2.field3[1] == 456") mustBe new PyBoolean(true)
        pi.eval("parameter1.field2.field3[2] == 789") mustBe new PyBoolean(true)
      }
    }

    "define structure with structures as fields parameter without data provided for one of fields" in {
      val mockTypeDefinitionService = mock[TypeDefinitionService]
      val typeCache = mockTypeDefinitionService.newTypeCache
      when(mockTypeDefinitionService.typeDefinitionLookup(Matchers.eq("id:structure"), Matchers.any())) thenReturn
        Future.successful(Some(Type(
          seq = Some("id:structure"),
          name = "Type2",
          fields = List(
            Field("field1", "sequence:CMYe"),
            Field("field2", "sequence:CMYe")
          )
        )))
      when(mockTypeDefinitionService.typeDefinitionLookup(Matchers.eq("sequence:CMYe"), Matchers.any())) thenReturn
        Future.successful(Some(Type(
          seq = Some("sequence:CMYe"),
          name = "Type1",
          fields = List(
            Field("field1", "string"),
            Field("field2", "int"),
            Field("field3", "list[int]")
          )
        )))

      val arguments = List(Argument("parameter1", "id:structure"))
      val requestData = Map(
        "parameter1" -> Seq(
          Right(Map(
            "field1" -> Seq(
              Right(Map(
                "field1" -> Seq(Left("OK")),
                "field2" -> Seq(Left("123")),
                "field3s" -> Seq(Right(Map("field3" -> Seq(Left("123"), Left("456"), Left("789")))))
              ))
            ),
            "wrong name" -> Seq(
              Right(Map(
                "field1" -> Seq(Left("OK")),
                "field2" -> Seq(Left("123")),
                "field3s" -> Seq(Right(Map("field3" -> Seq(Left("123"), Left("456"), Left("789")))))
              ))
            ),
          ))
        )
      )

      val codeFuture = ArgumentsInPython.generateArguments(arguments, requestData, mockTypeDefinitionService, typeCache)
      val code = Await.result(codeFuture, 5.seconds)

      utils.using(new PythonInterpreter()) { pi =>
        pi.exec(code)
        pi.eval("parameter1.field1.field1 == \"OK\"") mustBe new PyBoolean(true)
        pi.eval("parameter1.field1.field2 == 123") mustBe new PyBoolean(true)
        pi.eval("parameter1.field1.field3[0] == 123") mustBe new PyBoolean(true)
        pi.eval("parameter1.field1.field3[1] == 456") mustBe new PyBoolean(true)
        pi.eval("parameter1.field1.field3[2] == 789") mustBe new PyBoolean(true)

        pi.eval("parameter1.field2.field1 is None") mustBe new PyBoolean(true)
        pi.eval("parameter1.field2.field2 is None") mustBe new PyBoolean(true)
        pi.eval("parameter1.field2.field3 is None") mustBe new PyBoolean(true)
      }
    }

  }
}
