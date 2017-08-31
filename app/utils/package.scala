import play.api.data.Form

import scala.concurrent.{Promise, ExecutionContext, Future}
import scala.language.reflectiveCalls
import scala.util.{Failure, Success, Try}

package object utils {

  def using[A, B <: {def close() : Unit}](closeable: B)(f: B => A): A =
    try {
      f(closeable)
    }
    finally {
      closeable.close()
    }

  // --------------------------------------------------------------------------

  implicit class RichForm[A](form: Form[A]) {

    def continue[R](f: Try[A] => R): R =
      if (!form.hasErrors)
        f(Success(form.get))
      else {
        val errors = form.errors.foldLeft(new StringBuilder)((acc, err) => acc append err.message).toString()
        f(Failure(new IllegalArgumentException(errors)))
      }
  }
}
