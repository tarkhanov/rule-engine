package client

import org.querki.jquery.{JQuery, _}
import org.scalajs.dom.{Element, Event}

import scala.scalajs.js
import client.Popup.Modal

object Popup {

// This trait can be used to call showModal and showHide implemented in jquery.modal.js.
// jquery.modal.js was rewritten in scala.js in Modal class below
//  @js.native
//  trait ShowHide extends JQuery {
//    def showModal(): Unit = js.native
//    def hideModal(): Unit = js.native
//  }

  case class ModalOptions(top: String, overlay: Double)

  implicit class Modal(jQuery: JQuery) {

    private def closeModal(overlay: JQuery, modal: JQuery) = {
      overlay.fadeOut(200)
      modal.css(js.Dictionary[js.Any]("display" -> "none"))
    }

    def showModal(): Unit = {

      val options = ModalOptions(top = "10%", overlay = 0.5)
      var overlay = $("#modal_overlay")

      if (overlay.length == 0) {
        overlay = $("<div id='modal_overlay'></div>")
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
        $("body").append(overlay)
      }

      jQuery.each { item: Element =>

        val modal = $(item)
        val modal_height = modal.outerHeight()
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

        $("body").append(modal)

        modal.fadeTo(200, 1);
      }
    }

    def hideModal(): JQuery = {
      closeModal($("#modal_overlay"), jQuery)
    }
  }
}

class Popup(title: String) {

  private val content = $("""
    <div class="popup">
        <div><h2 class="header" style="margin: 0; background-color: #f8f8f8; padding: 30px 30px 24px 30px; text-align: center; border-top-right-radius: 4px; border-top-left-radius: 4px; border-bottom: 1px solid #ccc;"></h2></div>
        <div style="padding: 2em 2em 1.8em 2em;" class="popup-content"></div>
        <div style="text-align: right; padding: 20px; background-color: #f8f8f8; border-top: 2px solid #CCC; border-bottom-right-radius: 4px; border-bottom-left-radius: 4px;">
            <input type="submit" class="popup-apply" value="Apply" style="margin-right: 10px;" />
            <input type="submit" class="popup-cancel" value="Cancel" />
        </div>
    </div>
    """)

  content.css(js.Dictionary[js.Any](
    "border-radius" -> "4px",
    "background-color" -> "#fff",
    "box-shadow" -> "0 1px 4px #000",
    "width" -> "630px"
  ))

  content.find(".header").text(title)

  private val applyButton: JQuery = content.find(".popup-apply")
  applyButton.hide()

  content.find(".popup-cancel").click { e: Event =>
    content.hideModal() // asInstanceOf[ShowHide].hideModal()
    e.preventDefault()
  }

  private val wrapper = content.find(".popup-content")

  def withApply(callback: () => Unit): Unit = {
    applyButton.show()
    applyButton.click { e: Event =>
      callback()
      e.stopPropagation()
      e.preventDefault()
    }
  }

  def show(): Unit = content.showModal() //.asInstanceOf[ShowHide].showModal()

  def hide(): Unit = content.hideModal() // .asInstanceOf[ShowHide].hideModal()

  def update(value: JQuery): Unit = {
    wrapper.empty()
    value.appendTo(wrapper)
  }

}
