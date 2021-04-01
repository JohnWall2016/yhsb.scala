package yhsb.base.xml

import org.w3c.dom.Element
import org.w3c.dom.NodeList
import org.xml.sax.InputSource

import javax.xml.parsers.DocumentBuilderFactory
import java.io.StringReader

import scala.annotation.StaticAnnotation
import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.{typeOf, TypeTag}
import scala.reflect.{ClassTag, classTag}
import scala.annotation.meta._
import scala.annotation.Annotation
import scala.collection.mutable
import scala.tools.reflect.ToolBox

import yhsb.base.text.String._
import yhsb.base.reflect.Extension._
import yhsb.base.struct.MapField
import yhsb.base.collection.BiMap

sealed abstract trait XmlAnnotation extends StaticAnnotation

@field @getter @setter
case class Namespaces(namespaces: (String, String)*) extends XmlAnnotation {
  val bimap = new BiMap(namespaces: _*)

  def apply(name: String) = bimap.get(name)
}

@field @getter @setter
case class Node(name: String = null, filter: Element => Boolean = { _ => true })
  extends XmlAnnotation

@field @getter @setter
case class Attribute(name: String = null) extends XmlAnnotation

@field @getter @setter
case class AttrNode(name: String, attr: String = null) extends XmlAnnotation

@field @getter @setter
case class Text() extends XmlAnnotation

sealed trait TreeConvertible[T] {
  def apply(tree: ru.Tree): T
}

object TreeConvertible {
  implicit object NamespacesConvertible extends TreeConvertible[Namespaces] {
    def apply(tree: ru.Tree): Namespaces = {
      import ru._
      val list = tree.children.tail.collect {
        case Apply(
              TypeApply(
                Select(Apply(_, Literal(Constant(name: String)) :: Nil), _),
                _
              ),
              Literal(Constant(value: String)) :: Nil
            ) =>
          (name, value)
        case Apply(
              _,
              Literal(Constant(name: String)) :: Literal(
                Constant(value: String)
              ) :: Nil
            ) =>
          (name, value)
      }

      Namespaces(list: _*)
    }
  }

  implicit object TextConvertible extends TreeConvertible[Text] {
    def apply(tree: ru.Tree): Text = Text()
  }

  implicit object AttributeConvertible extends TreeConvertible[Attribute] {
    def apply(tree: ru.Tree): Attribute = {
      import ru._
      val Apply(_, Literal(Constant(name: String)) :: Nil) = tree
      Attribute(name)
    }
  }

  implicit object AttrNodeConvertible extends TreeConvertible[AttrNode] {
    def apply(tree: ru.Tree): AttrNode = {
      import ru._
      val Apply(
        _,
        Literal(Constant(name: String)) :: Literal(
          Constant(attr: String)
        ) :: Nil
      ) = tree
      AttrNode(name, attr)
    }
  }

  implicit object NodeConvertible extends TreeConvertible[Node] {
    def apply(tree: ru.Tree): Node = {
      import ru._
      val Apply(
        _,
        Literal(Constant(name: String)) :: func :: Nil
      ) = tree
      val tb = yhsb.base.reflect.Extension.mirror.mkToolBox()
      Node(name, tb.eval(tb.untypecheck(func)).asInstanceOf[Element => Boolean])
    }
  }
}

object Extension {
  implicit class ToElement(s: String) {
    def toElement: Element = {
      val inst = DocumentBuilderFactory.newInstance()
      inst.setNamespaceAware(true)
      inst
        .newDocumentBuilder()
        .parse(new InputSource(new StringReader(s)))
        .getDocumentElement()
    }
  }

  implicit class Annotations(list: List[ru.Annotation]) {
    def get[T: TreeConvertible: TypeTag]: Option[T] = {
      list
        .find(_.tree.tpe == typeOf[T])
        .map(it => implicitly[TreeConvertible[T]].apply(it.tree))
    }
  }

  implicit class RichNodeList(list: NodeList) extends Iterable[Element] {
    override def iterator: Iterator[Element] = {
      (for {
        i <- 0 until list.getLength()
        item = list.item(i)
        if item.isInstanceOf[Element]
      } yield (item.asInstanceOf[Element])).iterator
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

    def toObject[T: TypeTag]: T = {
      val inst = newInstance[T]

      inst.setters().foreach { case (name, info) =>
        val annos = info.symbol.annotations
        annos.get[Text] match {
          case Some(_) => invoke(info, element.getTextContent())
          case None =>
            annos.get[Attribute] match {
              case Some(attr) =>
                invoke(
                  info,
                  element.getAttribute(Option(attr.name).getOrElse(name))
                )
              case None =>
                annos.get[AttrNode] match {
                  case Some(attrNode) =>
                    val name_ = attrNode.name
                    val attr = Option(attrNode.attr).getOrElse(name)
                    invoke(info, element.getElementAttribute(name_, attr))
                  case None =>
                    annos.get[Node] match {
                      case Some(node) =>
                        val name_ = Option(node.name).getOrElse(name)
                        val elems = element.getElementsByTagName(name_)
                        if (elems != null) {
                          invoke(info, elems, inst, node.filter)
                        }
                      case None =>
                    }
                }
            }
        }
      }
      inst
    }

    def invoke(info: MethodInfo, value: String) = {
      if (info.paramList.isDefined) {
        val paramType = info.paramList.get(0)
        if (paramType <:< typeOf[MapField]) {
          val field = paramType.newInstance[MapField]
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

    def invoke[T: TypeTag](
        info: MethodInfo,
        nodeList: NodeList,
        inst: T,
        filter: Element => Boolean
    ) = {
      if (info.paramList.isDefined) {
        val paramType = info.paramList.get(0)
        if (paramType.erasure =:= typeOf[List[Any]]) {
          val list = mutable.ListBuffer[Any]()
          val ttag = toTypeTag[T](paramType.typeArgs(0))
          if (ttag != null) {
            nodeList.filter(filter).foreach { e =>
              val v = e.toObject(ttag)
              list.addOne(v)
            }
          }
          info.method(list.toList)
        } else {
          nodeList.find(filter) match {
            case Some(e) =>
              info.method(e.toObject(toTypeTag[Any](paramType)))
            case None =>
          }
        }
      }
    }
  }
}
