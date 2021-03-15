package yhsb.base.struct

import collection.mutable
import scala.annotation.meta.companionObject

abstract class MapField {
  private[base] var _value: String = null

  def value = _value

  def name = {
    if (valueMap.isDefinedAt(value)) {
      valueMap(value)
    } else {
      s"未知值: $value"
    }
  }

  def valueMap: PartialFunction[String, String]
  
  override def toString(): String = name
}

sealed class ListField[T] extends Iterable[T] {
  private[ListField] def this(items: T*) = {
    this()
    this.items.addAll(items)
  }

  private[base] val items = mutable.ListBuffer[T]()

  override def iterator: Iterator[T] = items.iterator

  def addOne(e: T) = items.addOne(e)

  def apply(index: Int) = items(index)

  override def size = items.size
}

object ListField {
  def apply[T](items: T*) = new ListField(items: _*)
}