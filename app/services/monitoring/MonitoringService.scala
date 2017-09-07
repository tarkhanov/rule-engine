package services.monitoring

import akka.NotUsed
import akka.stream.scaladsl.Flow
import play.api.libs.json.JsValue

trait MonitoringService {

  def join(): Flow[Nothing, JsValue, NotUsed]

}
