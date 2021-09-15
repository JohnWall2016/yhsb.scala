package yhsb.base.collection

import java.util.stream.Stream
import scala.jdk.CollectionConverters._

class BiMap[K, V](entries: (K, V)*) extends collection.immutable.Map[K, V] {
  private val map = collection.immutable.ListMap(entries: _*)

  override def iterator: Iterator[(K, V)] = map.iterator

  override def get(key: K): Option[V] = map.get(key)

  override def removed(key: K): Map[K, V] = map.removed(key)

  override def updated[V1 >: V](key: K, value: V1): Map[K, V1] =
    map.updated(key, value)

  lazy val invert = map.map { case (k, v) => (v, k) }

  def subBiMap(keys: Set[K]) = {
    val m = filter { case (k, _) => keys.contains(k) }
    new BiMap(m.toSeq: _*)
  }
}

object BiMap {
  def fromStream[K, V](stream: Stream[(K, V)]) = {
    new BiMap(stream.iterator.asScala.toList: _*)
  }

  def apply[K, V](entries: Seq[(K, V)]) = new BiMap(entries: _*)
}
