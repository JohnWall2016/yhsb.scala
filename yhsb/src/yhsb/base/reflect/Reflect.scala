package yhsb.base.reflect

import scala.reflect.runtime.universe._
import scala.reflect.ClassTag

object Extension {
  lazy val mirror =
    scala.reflect.runtime.currentMirror // runtimeMirror(getClass.getClassLoader)

  def newInstance[T: TypeTag](args: Any*): T = {
    val tpe = typeOf[T]
    val classSymbol = tpe.typeSymbol.asClass
    val classMirror = mirror.reflectClass(classSymbol)
    val ctorSymbol = tpe.decl(termNames.CONSTRUCTOR).asMethod
    val ctorMethod = classMirror.reflectConstructor(ctorSymbol)
    ctorMethod(args: _*).asInstanceOf[T]
  }

  implicit class RichType(tpe: Type) {
    def newInstance[T](args: Any*): T = {
      val classSymbol = tpe.typeSymbol.asClass
      val classMirror = mirror.reflectClass(classSymbol)
      val ctorSymbol = tpe.decl(termNames.CONSTRUCTOR).asMethod
      val ctorMethod = classMirror.reflectConstructor(ctorSymbol)
      ctorMethod(args: _*).asInstanceOf[T]
    }
  }

  case class MethodInfo(
      symbol: MethodSymbol,
      method: MethodMirror,
      paramList: Option[List[Type]]
  )

  implicit class RichInst[T: TypeTag: ClassTag](inst: T) {
    lazy val instanceMirror = mirror.reflect(inst)
    lazy val thisType = typeOf[T]

    def annotations: List[Annotation] = {
      thisType.typeSymbol.annotations
    }

    def methods(filter: MethodSymbol => Boolean) = {
      thisType.members.sorted.view.collect {
        case m: MethodSymbol if filter(m) => m
      }
    }

    def getParamList(list: List[List[Symbol]]) = {
      if (list.isEmpty) None
      else {
        Some(
          list(0).map(
            _.info.asSeenFrom(thisType, thisType.typeSymbol.asClass)
          )
        )
      }
    }

    def getters(filter: MethodSymbol => Boolean = { _ => true }) = {
      methods(m => m.isGetter && m.isPublic && filter(m)).map { m =>
        (
          m.getter.name.toString,
          MethodInfo(
            m,
            instanceMirror.reflectMethod(m),
            getParamList(m.paramLists)
          )
        )
      }.toMap
    }

    def setters(filter: MethodSymbol => Boolean = { _ => true }) = {
      methods(m => m.isSetter && m.isPublic && filter(m)).map { m =>
        (
          m.setter.name.toString.stripSuffix("_$eq"),
          MethodInfo(
            m,
            instanceMirror.reflectMethod(m),
            getParamList(m.paramLists)
          )
        )
      }.toMap
    }
  }
}
