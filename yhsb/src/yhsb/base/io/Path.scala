package yhsb.base.io

import java.io.File
import java.nio.file.{Path => JPath}
import java.nio.file.Paths

object Path {

  implicit class PathOps[T : PathConvertible](path: T) {
    def /[R : PathConvertible](rest: R) = {
      implicitly[PathConvertible[T]].apply(path)
        .resolve(
          implicitly[PathConvertible[R]].apply(rest)
        )
    }
  }

  implicit def toPath[T : PathConvertible](path: T) = {
    implicitly[PathConvertible[T]].apply(path)
  }

  sealed trait PathConvertible[T] {
    def apply(path: T): JPath
  }

  object PathConvertible {
    implicit object StringConvertible extends PathConvertible[String] {
      def apply(path: String): JPath = Paths.get(path)
    }
    
    implicit object FileConvertible extends PathConvertible[File] {
      def apply(path: File): JPath = Paths.get(path.getPath)
    }

    implicit object PathConvertible extends PathConvertible[JPath] {
      def apply(path: JPath): JPath = path
    }
  }
}