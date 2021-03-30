package yhsb.base.reflect

import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe._
import scala.reflect.ClassTag

object Extension {
  lazy val mirror = scala.reflect.runtime.currentMirror // ru.runtimeMirror(getClass.getClassLoader)

  def newInstance[T : TypeTag](args: Any*): T = {
    val tpe = typeOf[T]
    val classSymbol = tpe.typeSymbol.asClass
    val classMirror = mirror.reflectClass(classSymbol)
    val ctorSymbol = tpe.decl(ru.termNames.CONSTRUCTOR).asMethod
    val ctorMethod = classMirror.reflectConstructor(ctorSymbol)
    ctorMethod(args: _*).asInstanceOf[T]
  }

  case class FieldInfo(annotations: List[Annotation], method: MethodMirror)

  implicit class RichInst[T : TypeTag : ClassTag](inst: T) {
    def getGetters = {
      val instanceMirror = mirror.reflect(inst)
      typeOf[T].members.sorted.collect {
        case m: MethodSymbol if m.isGetter && m.isPublic =>
          (
            m.getter.name.toString, 
            FieldInfo(m.annotations, instanceMirror.reflectMethod(m))
          )
      }.toMap
    }

    def getSetters = {
      val instanceMirror = mirror.reflect(inst)
      typeOf[T].members.sorted.collect {
        case m: MethodSymbol if m.isSetter && m.isPublic =>
          (
            m.setter.name.toString.stripSuffix("_$eq"), 
            FieldInfo(m.annotations, instanceMirror.reflectMethod(m))
          )
      }.toMap
    }
  }
}