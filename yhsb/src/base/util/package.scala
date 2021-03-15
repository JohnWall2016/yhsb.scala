package yhsb.base
package object util {
  implicit class OptionalOps[T <: AnyRef](right: => T) {
    def ?:(left: T) = if (left != null) left else right
  }

  implicit class RichOps[T](t: T) {
    def let[R](f: T => R) = f(t)
  }
}