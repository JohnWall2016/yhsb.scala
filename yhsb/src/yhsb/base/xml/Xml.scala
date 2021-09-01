package yhsb.base.xml

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList

import org.xml.sax.InputSource

import java.io.StringReader
import java.io.StringWriter

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

import scala.annotation.StaticAnnotation
import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.{typeOf, TypeTag, Type}
import scala.annotation.meta._
import scala.annotation.Annotation
import scala.collection.mutable
import scala.tools.reflect.ToolBox
import scala.collection.mutable.LinkedHashMap

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
    def toObject[T: TypeTag]: T = toObject(typeOf[T])

    private def toObject[T](tpe: Type): T = {
      val inst = tpe.newInstance[T]
      val instType = tpe.instanceType(inst)
      instType.setters().foreach { case (name, info) =>
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
                      invoke(info, getElementAttribute(name_, attr))
                    case None =>
                      val (name_, fitler) =
                        annos.get[Node] match {
                          case Some(node) =>
                            (Option(node.name).getOrElse(name), node.filter)
                          case None =>
                            (name, { _: Element => true })
                        }
                      val elems = element.getElementsByTagName(name_)
                      if (elems != null) {
                        invoke(info, elems, fitler)
                      }
                  }
              }
          }
        }
      }
      inst
    }

    private def getElementAttribute(
        tagName: String,
        attrName: String
    ): String = {
      element.getElementsByTagName(tagName).foreach { element =>
        val attr = element.getAttribute(attrName)
        if (attr.nonNullAndEmpty) return attr
      }
      return null
    }

    private def invoke(info: MethodInfo, value: String) = {
      if (info.paramList.isDefined) {
        val paramType = info.paramList.get(0)
        if (paramType <:< typeOf[MapField]) {
          val field = paramType.newInstance[MapField]
          field.value = value
          info.method(field)
        } else if (paramType <:< typeOf[String]) {
          info.method(value)
        } else if (value != null && value != "") {
          if (paramType <:< typeOf[Int] || paramType <:< typeOf[Integer]) {
            info.method(value.toInt)
          } else if (paramType <:< typeOf[Double]) {
            info.method(value.toDouble)
          } else if (paramType <:< typeOf[BigDecimal]) {
            info.method(BigDecimal(value))
          } else if (paramType <:< typeOf[java.math.BigDecimal]) {
            info.method(BigDecimal(value).bigDecimal)
          }
        }
      }
    }

    private def invoke(
        info: MethodInfo,
        nodeList: NodeList,
        filter: Element => Boolean
    ) = {
      if (info.paramList.isDefined) {
        val paramType = info.paramList.get(0)
        if (paramType.erasure =:= typeOf[List[Any]]) {
          val list = mutable.ListBuffer[Any]()
          val argType = paramType.typeArgs.head
          nodeList.filter(filter).foreach { e =>
            val v = e.toObject[Any](argType)
            list.addOne(v)
          }
          info.method(list.toList)
        } else {
          nodeList.find(filter) match {
            case Some(e) =>
              info.method(e.toObject[Any](paramType))
            case None =>
          }
        }
      }
    }
  }

  implicit class RichDocument(doc: Document) {
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

    def toElement[T: TypeTag](
        inst: T,
        nodeName: String = null,
        namespaces: collection.Map[String, String] = null
    ): Element = toElement(inst, typeOf[T], nodeName, namespaces)

    private[xml] def toElement[T](
        inst: T,
        tpe: Type,
        nodeName: String,
        namespaces: collection.Map[String, String]
    ): Element = {
      val instType = tpe.instanceType(inst)
      val nodeName_ = if (nodeName != null) {
        nodeName
      } else {
        instType.annotations
          .get[Node]
          .map(_.name)
          .getOrElse(inst.getClass.getSimpleName())
      }
      val namespaces_ = if (namespaces != null) {
        namespaces
      } else {
        instType.annotations
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

      instType.getters().foreach { case (name, info) =>
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

                      val toElemChoose = { tpe: Type =>
                        if (tpe <:< typeOf[CustomElement]) {
                          { it: Any =>
                            it.asInstanceOf[CustomElement]
                              .toElement(
                                doc,
                                name_,
                                annos
                                  .get[Namespaces]
                                  .map(_.bimap)
                                  .getOrElse(null)
                              )
                          }
                        } else {
                          { it: Any =>
                            new ToXml[Any](it, tpe).toElement(
                              doc,
                              name_,
                              annos
                                .get[Namespaces]
                                .map(_.bimap)
                                .getOrElse(null)
                            )
                          }
                        }
                      }
                      info.method() match {
                        case list: List[Any] =>
                          val retType = instType.getType(
                            info.method.symbol.returnType.typeArgs(0)
                          )
                          val toElem = toElemChoose(retType)
                          nodes.addAll(list.map(toElem(_)))
                        case it =>
                          val retType =
                            instType.getType(info.method.symbol.returnType)
                          nodes.addOne(toElemChoose(retType)(it))
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
  }

  class ToXml[T] private[xml] (any: T, tpe: Type) {
    def toXml(indent: Boolean = false, declare: String = null): String = {
      val inst = DocumentBuilderFactory.newInstance()
      inst.setNamespaceAware(true)

      val doc = inst.newDocumentBuilder().newDocument()
      doc.appendChild(doc.toElement(any, tpe, null, null))

      doc.transformToString(declare, indent)
    }

    def toElement(
        doc: Document,
        nodeName: String,
        namespaces: collection.Map[String, String] = null
    ): Element = {
      doc.toElement(any, tpe, nodeName, namespaces)
    }
  }

  implicit def RichToXml[T: TypeTag](any: T) = new ToXml[T](any, typeOf[T])
}

trait CustomElement {
  def toElement(
      doc: Document,
      nodeName: String,
      namespaces: collection.Map[String, String]
  ): Element
}
