package yhsb.base.reflect

import scala.reflect.runtime.universe._
import scala.reflect.ClassTag
import scala.reflect.api
import scala.collection.mutable.LinkedHashMap

object Extension {
  lazy val mirror =
    scala.reflect.runtime.currentMirror // runtimeMirror(getClass.getClassLoader)

  def toClassTag[T: TypeTag] =
    ClassTag[T](mirror.runtimeClass(typeTag[T].tpe))

  def toClassTag[T](tpe: Type) =
    ClassTag[T](mirror.runtimeClass(tpe))

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

    def instanceType[T](inst: T) = {
      if (mirror.runtimeClass(tpe).isAssignableFrom(inst.getClass)) {
        new InstanceType(inst, tpe)
      } else {
        throw new Exception(s"$inst is not an instance of $tpe")
      }
    }
  }

  //implicit def ToInstanceType[T: TypeTag](inst: T) = typeOf[T].instanceType(inst)

  case class MethodInfo(
      symbol: MethodSymbol,
      method: MethodMirror,
      paramList: Option[List[Type]]
  )

  class InstanceType[T] private[reflect] (inst: T, tpe: Type) {
    lazy val instanceMirror = mirror.reflect(inst)(toClassTag[T](tpe))
    lazy val thisType = tpe

    def annotations: List[Annotation] = {
      thisType.typeSymbol.annotations
    }

    def methods(filter: MethodSymbol => Boolean) = {
      thisType.members.sorted.view.collect {
        case m: MethodSymbol if filter(m) => m
      }
    }

    def getType(tpe: Type): Type =
      tpe.asSeenFrom(thisType, thisType.typeSymbol.asClass)

    def getType(symbol: Symbol): Type = getType(symbol.info)

    private def getParamList(list: List[List[Symbol]]) = {
      if (list.isEmpty) None
      else Some(list(0).map(getType(_)))
    }

    def getters(filter: MethodSymbol => Boolean = { _ => true }) = {
      LinkedHashMap(
        methods(m => m.isGetter && m.isPublic && filter(m)).map { m =>
          (
            m.getter.name.toString,
            MethodInfo(
              m,
              instanceMirror.reflectMethod(m),
              getParamList(m.paramLists)
            )
          )
        }.toSeq: _*
      )
    }

    def setters(filter: MethodSymbol => Boolean = { _ => true }) = {
      LinkedHashMap(
        methods(m => m.isSetter && m.isPublic && filter(m)).map { m =>
          (
            m.setter.name.toString.stripSuffix("_$eq"),
            MethodInfo(
              m,
              instanceMirror.reflectMethod(m),
              getParamList(m.paramLists)
            )
          )
        }.toSeq: _*
      )
    }
  }
}
