package yhsb.base.reflective

import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe._
import scala.reflect.ClassTag

object Extension {
  val mirror = scala.reflect.runtime.currentMirror // ru.runtimeMirror(getClass.getClassLoader)

  implicit class RichType(tpe: ru.Type) {
    def getConstructor = {
      val classSymbol = tpe.typeSymbol.asClass
      val classMirror = mirror.reflectClass(classSymbol)
      val ctorSymbol = tpe.decl(ru.termNames.CONSTRUCTOR).asMethod
      val ctorMethod = classMirror.reflectConstructor(ctorSymbol)
      ctorMethod
    }
  }

  implicit class RichInst[T : TypeTag : ClassTag](inst: T) {
    def getGetters = {
      val instanceMirror = mirror.reflect(inst)
      typeOf[T].members.sorted.collect {
        case m: MethodSymbol if m.isGetter && m.isPublic =>
          (m.getter.name.toString(), instanceMirror.reflectMethod(m))
      }.toMap
    }

    def getSetters = {
      val instanceMirror = mirror.reflect(inst)
      typeOf[T].members.sorted.collect {
        case m: MethodSymbol if m.isSetter && m.isPublic =>
          (m.name, instanceMirror.reflectMethod(m))
      }.toMap
    }
  }
}