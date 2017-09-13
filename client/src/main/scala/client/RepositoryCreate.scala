package client

import org.querki.jquery._
import org.scalajs.dom.Event

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("RepositoryCreate")
object RepositoryCreate {

  @JSExport
  def main(): Unit = {

    $(".repositoryCreate").click { e: Event =>
      val popup = new Popup("Create...")
      popup.update($(
        """
            <div>
                <!--a class="create-folder-link" href="/private/newFolder">Create Folder</a-->
                <a class="create-type-link" href="/private/service/types/new">Create Type</a>
                <a class="create-ruleset-link" href="/private/service/rules/new">Create Rules</a>

                <div style="clear: both"></div>
            </div>
        """))
      popup.show()
      e.stopPropagation()
      e.preventDefault()
    }

  }
}
