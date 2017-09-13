package client

import org.querki.jquery.$
import org.scalajs.dom.{Element, Event, window}

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("Pager")
object Pager {

  @JSExport
  def main(): Unit = {
    $(".pageSizeSelect").change { (el: Element, e: Event) =>
      val link = $(el).data("link").map(_.toString).getOrElse("")
      window.location.replace(link.replace("{size}", $(el).`val`().toString))
      e.stopPropagation()
      e.preventDefault()
    }
  }

}
