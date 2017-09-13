package client

import org.querki.jquery._
import org.scalajs.dom.{Element, Event}
import org.scalajs.dom._
import scalatags.JsDom.all._
import scala.scalajs.js
import client.Popup.Modal
import org.scalajs.dom.raw.HTMLElement

object Popup {

// This trait can be used to call showModal and showHide implemented in jquery.modal.js.
// jquery.modal.js was rewritten in scala.js in Modal class below
//  @js.native
//  trait ShowHide extends JQuery {
//    def showModal(): Unit = js.native
//    def hideModal(): Unit = js.native
//  }

  case class ModalOptions(top: String, overlay: Double)

  implicit class Modal(popup: JQuery) {

    private def closeModal(overlay: JQuery, modal: JQuery) = {
      overlay.fadeOut(200)
      modal.css(js.Dictionary[js.Any]("display" -> "none"))
    }

    def showModal(): Unit = {

      val options = ModalOptions(top = "10%", overlay = 0.5)
      var overlay = $("#modal_overlay")

      if (overlay.length == 0) {
        overlay = $(div(id := "modal_overlay").render)
        overlay.css(js.Dictionary[js.Any](
          "position" -> "fixed",
          "z-index" -> "100",
          "top" -> "0px",
          "left" -> "0px",
          "height" -> "100%",
          "width" -> "100%",
          "background" -> "#000",
          "display" -> "none"
        ))
        overlay.appendTo(document.body)
      }

      popup.each { item: Element =>

        val modal = $(item)
        //val modal_height = modal.outerHeight()
        val modal_width = modal.outerWidth()

        overlay.css(js.Dictionary[js.Any]("display" -> "block", "opacity" -> 0))
        overlay.fadeTo(200, options.overlay)
        overlay.click { (el: Element, e: Event) =>
          closeModal($(el), modal)
        }

        modal.css(js.Dictionary[js.Any](
          "display" -> "block",
          "position" -> "fixed",
          "opacity" -> 0,
          "z-index" -> 11000,
          "left" -> (50 + "%"),
          "margin-left" -> (-(modal_width / 2) + "px"),
          "top" -> options.top
        ))

        modal.appendTo(document.body)

        modal.fadeTo(200, 1);
      }
    }

    def hideModal(): JQuery = {
      closeModal($("#modal_overlay"), popup)
    }
  }
}


class Popup(title: String) {

  private val applyButton = input(`type` := "submit", `class` := "popup-apply", value := "Apply").render
  private val jQueryApplyButton = $(applyButton)
  private val cancelButton = input(`type` := "submit", `class` := "popup-cancel", value := "Cancel").render
  private val wrapper = div(`class` := "popup-content").render

  private val content = $(
    div(`class` := "popup", style := "width: 630px",
      div(
        h2(`class` := "header",
          title
        )
      ),
      wrapper,
      div(`class` := "buttons",
        applyButton,
        cancelButton)
    ).render
  )

  jQueryApplyButton.hide()

  cancelButton.onclick = { e: Event =>
    hide()
    e.preventDefault()
  }

  def withApply(callback: () => Unit): Unit = {
    jQueryApplyButton.show()
    applyButton.onclick = { e: Event =>
      callback()
      e.stopPropagation()
      e.preventDefault()
    }
  }

  def show(): Unit = content.showModal()

  def hide(): Unit = {
    content.hideModal()
    content.detach()
  }

  def update(value: HTMLElement): Unit = {
    val jQueryWrapper = $(wrapper)
    jQueryWrapper.empty()
    jQueryWrapper.append(value)
  }

}
