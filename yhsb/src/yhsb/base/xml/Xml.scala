package yhsb.base.xml

import org.w3c.dom.Element
import scala.annotation.StaticAnnotation
import scala.reflect.runtime.{universe => ru}
import scala.annotation.meta._

@field @getter @setter
case class Namespaces(namespaces: Seq[String]) extends StaticAnnotation

@field @getter @setter
case class Node(name: String = "", filter: Element => Boolean) extends StaticAnnotation

@field @getter @setter
case class Attribute(name: String = "") extends StaticAnnotation

@field @getter @setter
case class AttrNode(name: String, attr: String = "") extends StaticAnnotation

@field @getter @setter
case class Text() extends StaticAnnotation

object Extension {
  implicit class RichElement(element: Element) {
    def toObject[T : ru.TypeTag](): T = {
      val typeTag = ru.typeTag[T]
      val mirror = ru.runtimeMirror(getClass.getClassLoader)



      ???
    }
  }
}