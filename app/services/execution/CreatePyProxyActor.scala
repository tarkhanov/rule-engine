package services.execution

import java.util
import javassist.{ClassClassPath, ClassPool, CtNewMethod}

import akka.actor.{Actor, ActorLogging}
import org.python.core.PyObject

import scala.collection.JavaConversions

/**
 * Created by Sergey Tarkhanov on 6/11/2015.
 */
case class CreatePyProxy(proxyClassName: String, pythonStructure: PythonStructure)

abstract class PyProxy extends PyObject {

  var link: util.Map[String, PyObject] = null

  def getLink :  util.Map[String, PyObject] = 
    link

  def this(link: PythonStructure) {
    this()
    this.link = JavaConversions.mapAsJavaMap(link)
  }

  override def toString: String =
    "PyProxy: " + link.toString

}

class CreatePyProxyActor(classLoader: ClassLoader) extends Actor with ActorLogging {

  private val pool = new ClassPool(false)
  pool.insertClassPath(new ClassClassPath(classOf[PyProxy]))

  def receive = {
    case CreatePyProxy(proxyClassName, pythonStructure) =>
      sender ! createProxy(proxyClassName, pythonStructure)
  }

  private def createProxy(proxyClassName: String, pythonStructure: PythonStructure): PyProxy = {
    log.debug("Loading Proxy Class: " + proxyClassName)

    val proxyClass = getLoadedProxyClass(proxyClassName)
      .orElse(createProxyClass(proxyClassName, pythonStructure))
      .getOrElse(throw new ClassNotFoundException("Unable to find or create proxy class with name " + proxyClassName))

    log.debug("Instance of Class: " + proxyClassName)
    proxyClass.getConstructor(classOf[PythonStructure]).newInstance(pythonStructure)
  }

  private def getLoadedProxyClass(proxyClassName: String): Option[Class[PyProxy]] = {
    try {
      val found = classLoader.loadClass(proxyClassName)
      Some(found.asInstanceOf[Class[PyProxy]])
    }
    catch {
      case e: ClassNotFoundException =>
        log.debug("Class not found: " + proxyClassName + " (But it's Ok)")
        None
    }
  }

  private def createProxyClass(proxyClassName: String, structure: PythonStructure): Option[Class[PyProxy]] = {

    var proxyClass = pool.getOrNull(proxyClassName)
    log.debug("Proxy Loaded " + proxyClass)

    if (proxyClass == null) {
      val superClass = pool.get(classOf[PyProxy].getName)
      proxyClass = pool.makeClass(proxyClassName, superClass)
      for ((property, _) <- structure) {
        val capitalized = property.substring(0, 1).toUpperCase + property.substring(1)
        val code = "public Object get" + capitalized + "() { return this.getLink().get(\"" + property + "\"); }"
        proxyClass.addMethod(CtNewMethod.make(code, proxyClass))
      }
    }

    val newProxyClass = pool.toClass(proxyClass, classLoader, null)
    Some(newProxyClass.asInstanceOf[Class[PyProxy]])
  }
}
