package yhsb.base.struct

import scala.collection.mutable
import scala.collection.immutable.AbstractSeq

abstract class MapField {
  private[base] var _value: String = null

  def value = _value

  private[yhsb] def value_=(v: String) = _value = v

  def name = {
    if (valueMap.isDefinedAt(value)) {
      valueMap(value)
    } else {
      s"未知值: $value"
    }
  }

  def valueMap: PartialFunction[String, String]
  
  override def toString: String = name
}

final class ListField[T] extends AbstractSeq[T] {
  private[ListField] def this(items: T*) = {
    this()
    this.items.addAll(items)
  }

  private[base] val items = mutable.ListBuffer[T]()

  override def iterator: Iterator[T] = items.iterator

  override def length: Int = items.length

  def addOne(e: T) = items.addOne(e)

  def apply(index: Int) = items(index)
}

object ListField {
  def apply[T](items: T*) = new ListField(items: _*)
}
