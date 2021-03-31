package yhsb.base.reflect

import java.lang.reflect.Method

object UnsafeAllocator {
  private var unsafe: Any = null
  private var allocateInstance: Method = null
  private var exception: Exception = null

  try {
    val unsafeClass = Class.forName("sun.misc.Unsafe")
    val f = unsafeClass.getDeclaredField("theUnsafe")
    f.setAccessible(true)
    unsafe = f.get(null)
    allocateInstance = unsafeClass.getMethod("allocateInstance", classOf[Class[_]])
  } catch {
    case e: Exception => exception = e
  }

  def newInstance[T](c: Class[T]): T =
    allocateInstance.invoke(unsafe, c).asInstanceOf[T]
}