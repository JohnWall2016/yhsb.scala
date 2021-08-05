package yhsb.base.struct

import scala.collection.mutable
import scala.collection.immutable.AbstractSeq
import scala.reflect.runtime.universe._

import yhsb.base.reflect.Extension
import com.google.gson.JsonObject

abstract class MapField {
  private[base] var _value: String = null

  def value = _value

  def setValue(value: String) = _value = value

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

  override def equals(other: Any): Boolean = {
    if (other == null || this.getClass != other.getClass) {
      false
    } else {
      other.asInstanceOf[MapField]._value == this._value
    }
  }

  override def hashCode(): Int = {
    (getClass, _value).##
  }
}

object MapField {
  abstract class Val[T <: MapField: TypeTag] {
    def Val(value: String): T = {
      val t = Extension.newInstance()
      t._value = value
      t 
    }
  }
}

final class ListField[T] extends AbstractSeq[T] {
  private[ListField] def this(items: T*) = {
    this()
    this.items.addAll(items)
  }

  private[base] val items = mutable.ListBuffer[T]()

  private var _unknownItems: mutable.ListBuffer[JsonObject] = null

  override def iterator: Iterator[T] = items.iterator

  override def length: Int = items.length

  def unknownItems = _unknownItems

  def addOne(e: T) = items.addOne(e)

  def addUnknownOne(e: JsonObject) = {
    if (_unknownItems == null) _unknownItems = mutable.ListBuffer(e)
    else _unknownItems.addOne(e)
  }

  def apply(index: Int) = items(index)
}

object ListField {
  def apply[T](items: T*) = new ListField(items: _*)
}

abstract class NotNull