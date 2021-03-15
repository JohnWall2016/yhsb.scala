package yhsb.base.collection

class BiMap[K, V](entries: (K, V)*) extends collection.immutable.Map[K, V] {
  private val map = Map(entries: _*)

  override def iterator: Iterator[(K, V)] = map.iterator

  override def get(key: K): Option[V] = map.get(key)

  override def removed(key: K): Map[K, V] = map.removed(key)

  override def updated[V1 >: V](key: K, value: V1): Map[K, V1] =
    map.updated(key, value)

  lazy val invert = map.map { case (k, v) => (v, k) }
}
