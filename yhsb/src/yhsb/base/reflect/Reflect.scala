package yhsb.base.reflect

import scala.reflect.runtime.universe._
import scala.reflect.ClassTag
import scala.reflect.api

object Extension {
  lazy val mirror =
    scala.reflect.runtime.currentMirror // runtimeMirror(getClass.getClassLoader)

  def toClassTag[T : TypeTag] =
    ClassTag[T](mirror.runtimeClass(typeTag[T].tpe))

  def toTypeTag[T](tpe: Type): TypeTag[T] =
    TypeTag(mirror, new api.TypeCreator {
      def apply[U <: api.Universe with Singleton](m: api.Mirror[U]) =
        if (m eq mirror) tpe.asInstanceOf[U # Type]
        else throw new IllegalArgumentException(
          s"Type tag defined in $mirror cannot be migrated to other mirrors."
        )
    })

  def newInstance[T: TypeTag](args: Any*): T = typeOf[T].newInstance(args: _*)

  def newInstance[T: TypeTag]: T = typeOf[T].newInstance

  implicit class RichType(tpe: Type) {
    def newInstance[T](args: Any*): T = {
      val classSymbol = tpe.typeSymbol.asClass
      val classMirror = mirror.reflectClass(classSymbol)
      val ctorSymbol = tpe.decl(termNames.CONSTRUCTOR).asMethod
      val ctorMethod = classMirror.reflectConstructor(ctorSymbol)
      ctorMethod(args: _*).asInstanceOf[T]
    }

    def newInstance[T]: T = {
      UnsafeAllocator.newInstance(
        mirror.runtimeClass(tpe.typeSymbol.asClass).asInstanceOf[Class[T]]
      )
    }
  }

  case class MethodInfo(
      symbol: MethodSymbol,
      method: MethodMirror,
      paramList: Option[List[Type]]
  )

  implicit class RichInst[T: TypeTag](inst: T) {
    lazy val instanceMirror = mirror.reflect(inst)(toClassTag[T])
    lazy val thisType = typeOf[T]

    def annotations: List[Annotation] = {
      thisType.typeSymbol.annotations
    }

    def methods(filter: MethodSymbol => Boolean) = {
      thisType.members.sorted.view.collect {
        case m: MethodSymbol if filter(m) => m
      }
    }

    def getType(symbol: Symbol) = {
      symbol.info.asSeenFrom(thisType, thisType.typeSymbol.asClass)
    }

    def getTypeTag[T](symbol: Symbol) = {
      toTypeTag[T](getType(symbol))
    }

    private def getParamList(list: List[List[Symbol]]) = {
      if (list.isEmpty) None
      else Some(list(0).map(getType(_)))
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
