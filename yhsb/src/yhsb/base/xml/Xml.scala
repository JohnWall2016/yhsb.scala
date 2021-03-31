package yhsb.base.xml

import org.w3c.dom.Element
import scala.annotation.StaticAnnotation
import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.{typeOf, TypeTag}
import scala.reflect.{ClassTag, classTag}
import scala.annotation.meta._
import yhsb.base.reflect.Extension._
import scala.annotation.Annotation
import yhsb.base.struct.MapField
import org.w3c.dom.NodeList
import yhsb.base.text.String._

@field @getter @setter
case class Namespaces(namespaces: Seq[String]) extends StaticAnnotation

@field @getter @setter
case class Node(name: Option[String] = None, filter: Element => Boolean)
  extends StaticAnnotation

@field @getter @setter
case class Attribute(name: Option[String] = None) extends StaticAnnotation

@field @getter @setter
case class AttrNode(name: String, attr: Option[String] = None) extends StaticAnnotation

@field @getter @setter
case class Text() extends StaticAnnotation

object Extension {
  implicit class Annotations(list: List[ru.Annotation]) {
    def get[T: ClassTag]: Option[T] = {
      list.find(classTag[T].runtimeClass.isInstance(_)).map(_.asInstanceOf[T])
    }
  }

  implicit class RichNodeList(list: NodeList) extends Iterable[Element] {
    override def iterator: Iterator[Element] = {
      (for {
        i <- 0 until list.getLength()
        item = list.item(i)
        if item.isInstanceOf[Element]
      } yield(item.asInstanceOf[Element])).iterator
    }
  }

  implicit class RichElement(element: Element) {
    def getElementAttribute(tagName: String, attrName: String): String = {
      element.getElementsByTagName(tagName).foreach { element =>
        val attr = element.getAttribute(attrName)
        if (attr.nonNullAndEmpty) return attr
      }
      return null
    }

    def toObject[T: TypeTag: ClassTag](): T = {
      val inst = newInstance[T]()

      inst.setters().foreach { case (name, info) =>
        val annos = info.symbol.annotations
        annos.get[Text] match {
          case Some(_) => invoke(info, element.getTextContent())
          case None =>
            annos.get[Attribute] match {
              case Some(attr) =>
                invoke(info, element.getAttribute(attr.name.getOrElse(name)))
              case None =>
                annos.get[AttrNode] match {
                  case Some(attrNode) =>
                    val name = attrNode.name
                    val attr = attrNode.attr.getOrElse(name)
                    invoke(info, element.getElementAttribute(name, attr))
                  case None =>
                }
            }
        }
      }
      ???
    }

    def invoke(info: MethodInfo, value: String) = {
      if (info.paramList.isDefined) {
        val paramType = info.paramList.get(0)
        if (paramType <:< typeOf[MapField]) {
          val field = paramType.newInstance[MapField]()
          field.value = value
          info.method(field)
        } else if (paramType <:< typeOf[Int] || paramType <:< typeOf[Integer]) {
          info.method(value.toInt)
        } else if (paramType <:< typeOf[BigDecimal]) {
          info.method(BigDecimal(value))
        } else if (paramType <:< typeOf[java.math.BigDecimal]) {
          info.method(BigDecimal(value).bigDecimal)
        } else if (paramType <:< typeOf[String]) {
          info.method(value)
        }
      }
    }
  }
}
