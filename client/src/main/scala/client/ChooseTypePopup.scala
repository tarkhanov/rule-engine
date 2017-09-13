package client

import org.querki.jquery._
import org.scalajs.dom.{Element, Event}

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scala.util.matching.Regex

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

  private def chooseTypePopup(currentType: String)(onChange: (String, String) => Unit): Popup = {
    val popup = new Popup(s"Type...")
    val container = $("""<div class="typeDefRoot"></div>""")
    val root = $("<div></div>")
    root.append(container)
    popup.update(root)

    var availableTypes = Map.empty[String, String]

    def drawSelector(currentType: String, container: JQuery): JQuery = {
      val typeSelect = $("""<select></select>""")
      for ((key, value) <- availableTypes) {
        val typeOption = $(s"""<option value="$value">$key</option> """)
        typeOption.appendTo(typeSelect)
      }
      $("""<option value="list">list</option> """).appendTo(typeSelect)
      $("""<option value="sequence">sequence</option> """).appendTo(typeSelect)
      $("""<option value="id">id</option> """).appendTo(typeSelect)
      typeSelect.appendTo(container)
      val param = $("""<span class="param"></span>""")
      param.appendTo(container)

      currentType match {
        case matchList(listType) =>
          typeSelect.`val`("list")
          drawList(listType, param)
        case matchSeq(seqType) =>
          typeSelect.`val`("sequence")
          drawSequence(seqType, param)
        case matchId(idType) =>
          typeSelect.`val`("id")
          drawId(idType, param)
        case _ =>
          typeSelect.`val`(currentType)
      }

      typeSelect.change { (el: Element, e: Event) =>
        val t = $(el).`val`().toString
        param.empty()
        if (t == "list")
          drawList("string", param)
        else if (t == "sequence")
          drawSequence("", param)
        else if (t == "id")
          drawId("", param)
      }
    }

    def drawSequence(seqType: String, container: JQuery): JQuery = {
      $(s""" <span>:<input type="text" value="$seqType" style="width: 140px; margin: 0 8px;" /></span> """)
        .appendTo(container)
    }

    def drawId(idType: String, container: JQuery): JQuery = {
      $(s""" <span>:<input type="text" value="$idType" style="width: 140px; margin: 0 8px;" /></span> """)
        .appendTo(container)
    }

    def drawList(listType: String, container: JQuery): JQuery = {
      val list = $(""" <span>[<span class="listParam"></span>]</span> """)
      val param = list.find(".listParam")
      drawSelector(listType, param)
      list.appendTo(container)
    }

    $.getJSON("/private/service/types/available.ajax?list=builtin", success = { (o: js.Object, _: String, _: JQueryXHR) =>
      container.empty()
      val d = o.asInstanceOf[js.Array[js.Object]]
      $.each(d, { (item: js.Object) =>
        val tn = item.asInstanceOf[TypeAndName]
        availableTypes += tn.name -> tn.`type`
      })
      drawSelector(currentType, container)
    })

    def readType(select: JQuery): String = {
      val selectValue = select.`val`().toString
      if (selectValue == "list") {
        val paramSelect = select.parent().children(".param").children().children().children("select")
        "list[" + readType(paramSelect) + "]"
      }
      else if (selectValue == "sequence") {
        val input = select.parent().children(".param").children().children("INPUT").valueString
        "sequence:" + input
      }
      else if (selectValue == "id") {
        val input = select.parent().children(".param").children().children("INPUT").valueString
        "id:" + input
      }
      else
        selectValue
    }

    popup.withApply { () =>
      val select = container.children("SELECT")
      val `type` = readType(select)
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
