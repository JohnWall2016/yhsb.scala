package yhsb.base.net

import scala.collection.mutable

class HttpHeader extends Iterable[(String, String)] {

  private val header =
    mutable.LinkedHashMap[String, mutable.ListBuffer[String]]()

  override def iterator: Iterator[(String, String)] = {
    (for ((k, v) <- header; s <- v) yield (k, s)).iterator
  }

  def contains(key: String): Boolean =
    header.keySet.exists(_.toLowerCase() == key.toLowerCase())

  def apply(key: String): Iterable[String] =
    header.find(_._1.toLowerCase() == key.toLowerCase()).map(_._2).get

  def add(key: String, value: String) = {
    val k = key
    if (!contains(k)) {
      header(k) = mutable.ListBuffer()
    }
    header(k).addOne(value)
  }

  def get(key: String): Option[Iterable[String]] =
    header.find(_._1.toLowerCase() == key.toLowerCase()).map(_._2)

  def addAll(other: HttpHeader) = {
    for ((k, v) <- other) {
      add(k, v)
    }
  }

  def remove(key: String) = {
    header.keySet.find(_.toLowerCase() == key.toLowerCase()) match {
      case None        =>
      case Some(value) => header.remove(value)
    }
  }

  def clear() = header.clear()

  override def toString() = {
    this.map(it => s"${it._1}:${it._2}").mkString("\r\n")
  }
}
