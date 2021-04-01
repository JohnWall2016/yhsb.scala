package yhsb.base.xml

import org.w3c.dom.Element
import org.w3c.dom.NodeList
import org.xml.sax.InputSource

import javax.xml.parsers.DocumentBuilderFactory
import java.io.StringReader

import scala.annotation.StaticAnnotation
import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.{typeOf, TypeTag, typeTag}
import scala.reflect.{ClassTag, classTag}
import scala.annotation.meta._
import scala.annotation.Annotation
import scala.collection.mutable
import scala.tools.reflect.ToolBox

import yhsb.base.text.String._
import yhsb.base.reflect.Extension._
import yhsb.base.struct.MapField
import yhsb.base.collection.BiMap
import org.w3c.dom.Document
import java.io.StringWriter
import javax.xml.transform.TransformerFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import scala.collection.mutable.LinkedHashMap

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

    def defined[T: TypeTag]: Boolean = {
      list.find(_.tree.tpe == typeOf[T]).isDefined
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
        if (!annos.defined[transient]) {
          annos.defined[Text] match {
            case true => invoke(info, element.getTextContent())
            case false =>
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

  implicit class RichDocument(doc: Document) {
    def toElement[T: TypeTag](
        inst: T,
        nodeName: String = null,
        namespaces: collection.Map[String, String] = null
    ): Element = {
      val nodeName_ = if (nodeName != null) {
        nodeName
      } else {
        inst.annotations
          .get[Node]
          .map(_.name)
          .getOrElse(inst.getClass.getSimpleName())
      }
      val namespaces_ = if (namespaces != null) {
        namespaces
      } else {
        inst.annotations
          .get[Namespaces]
          .map(_.bimap)
          .getOrElse(Map[String, String]())
      }
      val attributes = LinkedHashMap(
        namespaces_.map { case (key, value) =>
          (
            if (key.isNullOrEmpty) "xmlns" else s"xmlns:$key",
            value
          )
        }.toSeq: _*
      )
      var text: String = null

      val nodes = mutable.ListBuffer[Element]()

      inst.getters().foreach { case (name, info) =>
        val annos = info.symbol.annotations
        if (!annos.defined[transient]) {
          annos.defined[Text] match {
            case true =>
              text = info.method().toString
            case false =>
              annos.get[Attribute] match {
                case Some(attr) =>
                  val name_ = if (attr.name.nonNullAndEmpty) attr.name else name
                  attributes(name_) = info.method().toString
                case None =>
                  annos.get[Node] match {
                    case Some(node) =>
                      val name_ =
                        if (node.name.nonNullAndEmpty) node.name else name

                      info.method() match {
                        case list: List[Any] =>
                          val ttag = inst.getTypeTag[Any](
                            info.method.symbol.returnType.typeArgs(0)
                          )

                          nodes.addAll(list.map { it =>
                            val toXml = new RichToXml[Any](it)(ttag)
                            toXml.toElement(
                              doc,
                              name_,
                              annos.get[Namespaces].map(_.bimap).getOrElse(null)
                            )
                          })
                        case it =>
                          val ttag =
                            inst.getTypeTag[Any](info.method.symbol.returnType)

                          val toXml = new RichToXml[Any](it)(ttag)
                          nodes.addOne(
                            toXml.toElement(
                              doc,
                              name_,
                              annos.get[Namespaces].map(_.bimap).getOrElse(null)
                            )
                          )
                      }
                    case None =>
                      annos.get[AttrNode] match {
                        case Some(attrNode) =>
                          val name_ = attrNode.name
                          val attr =
                            if (attrNode.attr.nonNullAndEmpty) attrNode.attr
                            else name
                          nodes.addOne(
                            doc.createElement(
                              name_,
                              Map(attr -> info.method().toString())
                            )
                          )
                        case None =>
                      }
                  }
              }
          }
        }
      }

      if (attributes.nonEmpty || text != null || nodes.nonEmpty) {
        val elem = createElement(nodeName_, attributes, text)
        nodes.foreach { it =>
          elem.appendChild(it)
        }
        elem
      } else {
        createElement(nodeName_)
      }
    }

    def transformToString(
        declare: String = null,
        indent: Boolean = false
    ): String = {
      val transformer = TransformerFactory.newInstance().newTransformer()
      if (declare != null) {
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
      }
      transformer.setOutputProperty(
        OutputKeys.INDENT,
        if (indent) "yes" else "no"
      )

      val writer = new StringWriter()
      if (declare != null) writer.write(declare)

      transformer.transform(new DOMSource(doc), new StreamResult(writer))
      writer.getBuffer().toString()
    }

    def createElement(
        name: String,
        attributes: collection.Map[String, String] = null,
        text: String = null,
        namesapces: collection.Map[String, String] = null
    ): Element = {
      val element = doc.createElement(name)
      if (attributes != null) {
        attributes.foreach { case (key, value) =>
          element.setAttribute(key, value)
        }
      }
      if (namesapces != null) {
        namesapces.foreach { case (key, value) =>
          element.setAttribute(key, value)
        }
      }
      if (text != null) {
        element.appendChild(doc.createTextNode(text))
      }
      element
    }
  }

  implicit class RichToXml[T: TypeTag](any: T) {
    def toXml(indent: Boolean = false, declare: String = null): String = {
      val inst = DocumentBuilderFactory.newInstance()
      inst.setNamespaceAware(true)

      val doc = inst.newDocumentBuilder().newDocument()
      doc.appendChild(doc.toElement(any))

      doc.transformToString(declare, indent)
    }

    def toElement(
        doc: Document,
        nodeName: String,
        namespaces: collection.Map[String, String] = null
    ): Element = {
      doc.toElement(any, nodeName, namespaces)
    }
  }
}