package client

import org.querki.jquery._
import org.scalajs.dom.{Element, Event, window}

import scala.scalajs.js.Dictionary
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("TypeEditor")
object TypeEditor {

  private def extendTypeSelector(newItem: JQuery): Unit =
    ChooseTypePopup.extendTypeSelector(newItem)

  @JSExport
  def main(): Unit = {

    def extendArgumentItem(item: JQuery) = {
      item.find(".delete").click { e: Event =>
        item.hide("fast", () => item.remove())
        e.stopPropagation()
        e.preventDefault()
      }
    }

    $(".type-fields").each { item: Element =>
      val args = $(item)
      val argsList = args.find(".field-list")
      argsList.find(".field").each { item: Element => extendArgumentItem($(item)) }
      args.find(".field-add").click { e: Event =>
        val newItem = $(
          """
            <div class="field">
                <div class="fieldName"><input type="text" class="name" value="" /></div>
                <div class="fieldType"><a href="#" class="type" data-type="string">string</a></div>
                <div class="fieldActions">
                    <a href="#" class="up">U</a> <a href="#" class="down">D</a> <a href="#" class="delete"><span>R</span></a>
                </div>
                <div style="clear: left;"></div>
            </div>
            """)
        extendArgumentItem(newItem)
        //js.Dynamic.global.extendTypeSelector(newItem)
        extendTypeSelector(newItem)
        newItem.hide().appendTo(argsList).show("fast")
        e.stopPropagation()
        e.preventDefault()
      }
    }

    $(".save-type").click { e: JQueryEventObject =>

      val recordId = $(".recordId").text()
      val name = $("INPUT.typeName").valueString
      val sequence = $("INPUT.typeSequence").valueString
      val csrfToken = $("INPUT[name=csrfToken]").valueString

      val fields = $(".field-list").find(".field").toArray().map { item: Element =>
        val fieldName = $(item).find(".name").valueString
        val fieldType = $(item).find(".type").data("type").getOrElse("int")
        s""" { "name": "$fieldName", "type": "$fieldType" } """
      }.mkString(", ")
      val json = s"""{ "seq": "$sequence", "name": "$name", "fields": [ $fields ] } """

      $.ajax(JQueryAjaxSettings
        .url("/private/service/types/" + recordId + "/save")
        .data(json).contentType("application/json")
        .headers(Dictionary("Csrf-Token" -> csrfToken))
        .`type`("POST")
        .success { (_: Any, _: String, _: JQueryXHR) => window.location.replace("/private") }
        .error { (_: JQueryXHR, _: String, _: String) => window.alert("Unable to save type") }._result)

      e.stopPropagation()
      e.preventDefault()
    }
  }
}