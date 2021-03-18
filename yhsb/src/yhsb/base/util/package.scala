package yhsb.base
package object util {
  implicit class OptionalOps[T <: AnyRef](right: => T) {
    def ?:(left: T) = if (left != null) left else right
  }

  implicit class UtilOps[T](t: T) {
    def let[R](f: T => R) = f(t)

    def set(f: T => Unit) = {
      f(t)
      t
    }

    def in(iter: Iterable[T]) = iter.exists(_ == t)

    def in(iter: Option[Iterable[T]]) = iter.exists(_.exists(_ == t))
  }
}