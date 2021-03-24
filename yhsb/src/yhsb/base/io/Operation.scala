package yhsb.base.io

object AutoClose {
  def use[A <: AutoCloseable, B](closable: A)(f: A => B): B = {
    try {
      f(closable)
    } finally {
      closable.close()
    }
  }
}
