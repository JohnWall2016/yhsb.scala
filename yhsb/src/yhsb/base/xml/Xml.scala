package yhsb.base.xml

import org.w3c.dom.Element
import scala.annotation.StaticAnnotation
import scala.reflect.runtime.{universe => ru}

case class Namespaces(namespaces: Seq[String]) extends StaticAnnotation

case class Node(name: String = "", filter: Element => Boolean) extends StaticAnnotation

case class Attribute(name: String = "") extends StaticAnnotation

case class AttrNode(name: String, attr: String = "") extends StaticAnnotation

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