package services.monitoring

import java.lang.management.ManagementFactory
import java.util.Date
import javax.management.{Attribute, ObjectName}

import play.api.libs.json.{JsObject, JsString, Json}

object MonitoringUtil {

  def collectStatusInfo(): JsObject = {

    def getProcessCpuLoad: (Int, Int) = {

      val mbs = ManagementFactory.getPlatformMBeanServer
      val name = ObjectName.getInstance("java.lang:type=OperatingSystem")
      val list = mbs.getAttributes(name, List("SystemCpuLoad", "ProcessCpuLoad").toArray)

      val att = list.get(0).asInstanceOf[Attribute]
      val systemCPULoad = att.getValue.asInstanceOf[Double]
      val att1 = list.get(1).asInstanceOf[Attribute]
      val processCPULoad = att1.getValue.asInstanceOf[Double]

      def toPercentage(value: Double): Int = {
        val res = if (value == -1.0) Float.NaN
        else ((value * 1000) / 10.0f).toFloat
        math.round(math.floor(res)).toInt
      }

      (toPercentage(systemCPULoad), toPercentage(processCPULoad))
    }

    val runtime = Runtime.getRuntime
    val mb = 1024 * 1024
    val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / mb
    val totalMemory = runtime.totalMemory() / mb

    val (systemLoad, processLoad) = getProcessCpuLoad

    Json.obj(
      "ts" -> JsString(new Date().getTime.toString),
      "sl" -> JsString(systemLoad.toString),
      "pl" -> JsString(processLoad.toString),
      "um" -> JsString(usedMemory.toString),
      "tm" -> JsString(totalMemory.toString)
    )
  }
}
