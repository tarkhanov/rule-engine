package client

import org.querki.jquery._
import org.scalajs.dom.Event
import scalatags.JsDom.all._
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("RepositoryCreate")
object RepositoryCreate {

  private def popupContent =
    div(
      a(`class` := "create-type-link", href := "/private/service/types/new", "Create Type"),
      a(`class` := "create-ruleset-link", href := "/private/service/rules/new", "Create Rules"),
      div(style := "clear: both")
    ).render

  @JSExport
  def main(): Unit = {

    $(".repositoryCreate").click { e: Event =>
      val popup = new Popup("Create...")
      popup.update(popupContent)
      popup.show()
      e.stopPropagation()
      e.preventDefault()
    }
  }
}
