package client

import org.querki.jquery._
import org.scalajs.dom.raw.HTMLElement
import org.scalajs.dom.{Element, Event, window}

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scala.util.matching.Regex
import scalatags.JsDom.all._

@JSExportTopLevel("ChooseTypePopup")
object ChooseTypePopup {

  def extendTypeSelector(root: JQuery): Unit = {
    root.find("A.type").click { (el: Element, e: Event) =>
      val row = $(el)
      val initialType: String = row.data("type").map(_.toString).getOrElse("int")
      val popup = chooseTypePopup(initialType) { (`type`: String, name: String) =>
        row.data("type", `type`)
        row.text(name)
      }
      popup.show()
      e.stopPropagation()
      e.preventDefault()
    }
  }

  private val matchList: Regex = "^list\\[(.+)\\]$".r //.exec currentType
  private val matchSeq: Regex = "^sequence:(.+)$".r //.exec currentType
  private val matchId: Regex = "^id:(.+)$".r //.exec currentType

  @js.native
  trait TypeAndName extends js.Object {
    def name: String = js.native
    def `type`: String = js.native
  }

  private def drawSelector(currentType: String, availableTypes: Map[String, String]): Seq[HTMLElement] = {

    val typeSelect = select(
      availableTypes.map { case (typeName, typeValue) =>
        option(value := typeValue, typeName)
      }.toSeq,
      option(value := "list", "list"),
      option(value := "sequence", "sequence"),
      option(value := "id", "id")
    ).render

    val param = span(`class` := "param").render

    currentType match {
      case matchList(listType) =>
        typeSelect.value = "list"
        param.appendChild(drawList(listType, availableTypes).render)
      case matchSeq(seqType) =>
        typeSelect.value = "sequence"
        param.appendChild(drawSequence(seqType).render)
      case matchId(idType) =>
        typeSelect.value = "id"
        param.appendChild(drawId(idType).render)
      case _ =>
        typeSelect.value = currentType
    }

    typeSelect.onchange = { (_: Event) =>
      $(param).empty()
      $(typeSelect).`val`().toString match {
        case "list" => param.appendChild(drawList("string", availableTypes).render)
        case "sequence" => param.appendChild(drawSequence("").render)
        case "id" => param.appendChild(drawId("").render)
        case _ =>
      }
    }

    Seq(typeSelect, param)
  }

  private def drawSequence(seqType: String) =
    span(
      stringFrag(":"),
      input(`type` := "text", value := seqType, style := "width: 140px; margin: 0 8px;")
    )

  private def drawId(idType: String) =
    span(
      ":",
      input(`type` := "text", value := idType, style := "width: 140px; margin: 0 8px;")
    )

  private def drawList(listType: String, availableTypes: Map[String, String]) =
    span(
      "[",
      span(`class` := "listParam", drawSelector(listType, availableTypes)),
      "]"
    )

  private def chooseTypePopup(currentType: String)(onChange: (String, String) => Unit): Popup = {
    val popup = new Popup(s"Type...")
    val container = div(`class` := "typeDefRoot").render
    popup.update(container)

    var availableTypes = Map.empty[String, String]
    $.getJSON("/private/service/types/available.ajax?list=builtin", success = { (o: js.Object, _: String, _: JQueryXHR) =>
      $(container).empty()
      val types = o.asInstanceOf[js.Array[TypeAndName]]
      $.each(types, { item: TypeAndName => availableTypes += item.name -> item.`type` })
      container.appendChild(drawSelector(currentType, availableTypes).render)
    }).fail { (_: JQueryXHR, _: String, _: String) =>
      window.alert("Unable to load data types")
    }

    def readType(select: JQuery): String = {
      select.`val`().toString match {
        case "list" =>
          "list[" + readType(select.parent().children(".param").find("select").first()) + "]"
        case "sequence" =>
          "sequence:" + select.parent().children(".param").find("INPUT").first().valueString
        case "id" =>
          "id:" + select.parent().children(".param").find("INPUT").first().valueString
        case selectValue =>
          selectValue
      }
    }

    popup.withApply { () =>
      val `type` = readType($(container).find("SELECT").first())
      onChange(`type`, `type`)
      popup.hide()
    }

    popup
  }

  @JSExport
  def main(): Unit = {
    extendTypeSelector($("BODY"))
  }

}
