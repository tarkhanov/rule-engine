package client

import org.querki.jquery.JQuery
import org.scalajs.dom.{Element, Event, window}
import org.querki.jquery._

import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("RuleSetEditor")
object RuleSetEditor {

  @JSExport
  def main(): Unit = {
    ruleSetArguments()
    ruleSetResults()
    ruleSetRules()
    ruleSetSave()
  }

  def ruleSetArguments(): Unit = {

    def extendArgumentItem(item: JQuery) = {
      item.find(".delete").click { e: Event =>
        item.hide("fast", () => item.remove())
        e.stopPropagation()
        e.preventDefault()
      }
    }

    $(".container-args").each { item: Element =>
      val args = $(item)
      val argsList = args.find(".args-list")
      argsList.children().each { item: Element =>
        extendArgumentItem($(item))
      }
      args.find(".args-add").click { e: Event =>
        val newItem = $(
          """
            <div class="arg">
                <div class="argumentName" style="float: left; width: 50%;">
                    <input type="text" style="width: 80%;" value=""/>
                </div>
                <div class="typeName" style="float: left; width: 40%;">
                    <a href="#" class="type" data-type="string">string</a>
                </div>
                <div style="float: left; width: 10%; text-align: right;">
                    <a class="up" href="#"><span>U</span></a>
                    <a class="down" href="#"><span>D</span></a>
                    <a class="delete" href="#"><span>X</span></a>
                </div>
                <div style="clear: both;"></div>
            </div>
            """)
        extendArgumentItem(newItem)
        ChooseTypePopup.extendTypeSelector(newItem)
        newItem.hide().appendTo(argsList).show("fast")
        e.stopPropagation()
        e.preventDefault()
      }
    }
  }

  def ruleSetResults(): Unit = {
    def extendResultItem(item: JQuery) = {
      item.find(".delete").click { e: Event =>
        item.hide("fast", () => item.remove())
        e.preventDefault()
      }
    }

    $(".container-results").each { item: Element =>
      val results = $(item)
      val resultsList = results.find(".results-list")
      resultsList.children().each { item: Element =>
        extendResultItem($(item))
      }
      results.find(".results-add").click { e: Event =>
        val newItem = $(
          """
            <div class="result">
                <div class="resultName" style="float: left; width: 50%;">
                    <input type="text" style="width: 80%;" value=""/>
                </div>
                <div class="typeName" style="float: left; width: 40%;">
                    <a href="#" class="type" data-type="string">string</a>
                </div>
                <div style="float: left; width: 10%; text-align: right;">
                    <a class="up" href="#"><span>U</span></a>
                    <a class="down" href="#"><span>D</span></a>
                    <a class="delete" href="#"><span>X</span></a>
                </div>
                <div style="clear: both;"></div>
            </div>
            """)
        extendResultItem(newItem)
        ChooseTypePopup.extendTypeSelector(newItem)
        newItem.hide().appendTo(resultsList).show("fast")
        e.stopPropagation()
        e.preventDefault()
      }
    }
  }

  def ruleSetRules(): Unit = {

    def extendRulesItem(item: JQuery) = {
      item.find(".delete").click { e: Event =>
        item.hide("fast", () => item.remove())
        e.stopPropagation()
        e.preventDefault()
      }
    }

    $(".container-rules").each { item: Element =>
      val rules = $(item)
      val rulesList = rules.find(".rules-list")
      rulesList.children().each { item: Element =>
        extendRulesItem($(item))
      }
      rules.find(".rules-add").click { e: Event =>
        val newItem = $(
          """
            <div style="margin-bottom: 10px; background-color: #FFF;border: 2px solid #FFF; border-radius: 2px; /*box-shadow: 0 1px 1px 1px #A0A0A0;*/">
                <table class="item" style="width: 100%;">
                    <tr>
                        <th width="2%"><div class="fieldHeader">N</div></th>
                        <td width="98%">
                            <table width="100%">
                                <tr>
                                    <td>
                                        <div class="fieldFrame">
                                            <input class="ruleName" type="text" value="" />
                                        </div>
                                    </td>
                                    <td style="padding: 11px;">
                                        <a class="delete" href="#"><span>X</span></a>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    <tr>
                        <th><div class="fieldHeader">?</div></th>
                        <td><div class="fieldFrame">
                            <textarea  class="ruleCondition" style="width: 100%; height: 160px;"></textarea>
                        </div></td>
                    </tr>
                    <tr>
                        <th><div class="fieldHeader">=</div></th>
                        <td><div class="fieldFrame">
                            <textarea class="ruleBody" style="width: 100%; height: 160px;"></textarea>
                        </div></td>
                    </tr>
                </table>
                </div>
            """)
        extendRulesItem(newItem)
        newItem.hide().appendTo(rulesList).show("fast")
        e.stopPropagation()
        e.preventDefault()
      }
    }
  }

  def ruleSetSave(): Unit = {
    $(".save-button").click { e: Event =>
      val recordId = $(".recordId").text()
      val name = $("INPUT.rulesName").`val`()
      val sequence = $("INPUT.rulesSequence").`val`()
      val csrfToken = $("INPUT[name=csrfToken]").`val`().toString

      val args = $(".args-list").find(".arg").toArray().map { item: Element =>
        val name = $(item).find(".argumentName INPUT").`val`()
        val `type` = $(item).find(".typeName A").data("type")
        s""" { "name": "$name", "type": "${`type`}" } """
      }.mkString(", ")

      val results = $(".results-list").find(".result").toArray().map { item: Element =>
        val name = $(item).find(".resultName INPUT").`val`()
        val `type` = $(item).find(".typeName A").data("type")
        s""" { "name": "$name", "type": "${`type`}" } """
      }.mkString(", ")

      val rules = $(".rules-list").find(".item").toArray().map { item: Element =>
        val name = JSON.stringify($(item).find(".ruleName").`val`())
        val condition = JSON.stringify($(item).find(".ruleCondition").`val`())
        val body = JSON.stringify($(item).find(".ruleBody").`val`())
        s""" { "name": $name, "condition": $condition, "body": $body } """
      }.mkString(", ")

      val json = s"""{ "seq": "$sequence", "name": "$name", "arguments": [ $args ], "results": [ $results ], "rules": [ $rules ] } """

      $.ajax(JQueryAjaxSettings
        .url("/private/service/rules/" + recordId + "/save")
        .`type`("POST")
        .headers(js.Dictionary[String]("Csrf-Token" -> csrfToken))
        .contentType("application/json")
        .data(json)
        .success { (_: Any, _: String, _: JQueryXHR) => window.location.replace("/private") }
        .error { (_: JQueryXHR, _: String, _: String) => window.alert("Unable to save rules") }._result
      )

      e.stopPropagation()
      e.preventDefault()
    }
  }
}

