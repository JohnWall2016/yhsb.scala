package yhsb.net

import scala.collection.mutable

class HttpHeader extends Iterable[(String, String)] {

  private val header = mutable.Map[String, mutable.ListBuffer[String]]()

  override def iterator: Iterator[(String, String)] = {
    (for ((k, v) <- header; s <- v)  yield (k, s)).iterator
  }

  def contains(key: String): Boolean = header.contains(key.toLowerCase())

  def add(key: String, value: String) {
    val k = key.toLowerCase()
    if (!header.contains(k)) {
      header(k) = mutable.ListBuffer()
    }
    header(k).addOne(value)
  }

  def get(key: String): Option[Iterable[String]] = header.get(key.toLowerCase())

  def addAll(other: HttpHeader) {
    for ((k, v) <- other) {
      add(k, v)
    }
  }

  def remove(key: String) = header.remove(key.toLowerCase())

  def clear() = header.clear()
}