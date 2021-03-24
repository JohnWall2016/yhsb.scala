package yhsb.base.io

object AutoClose {
  def use[A <: AutoCloseable, B](closeable: A)(f: A => B): B = {
    try {
      f(closeable)
    } finally {
      closeable.close()
    }
  }

  implicit class AutoCloseOps[A <: AutoCloseable](closeable: A) {
    def use[B](f: A => B): B = {
      AutoClose.use(closeable)(f)
    }
  }
}
