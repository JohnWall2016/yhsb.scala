package yhsb.util

object Optional {
  implicit class OptionalOps[T <: AnyRef](right: T) {
    def ?:(left: T) = if (left != null) left else right
  }
}
