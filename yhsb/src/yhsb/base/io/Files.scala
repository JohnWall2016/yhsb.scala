package yhsb.base.io

import java.io.File
import java.nio.file.{Path => JPath}
import java.nio.file.Paths

import scala.util.matching.Regex

object Files {
  def trimExtension(fileName: String) = {
    val index = fileName.lastIndexOf(".")
    if (index >= 0) {
      fileName.substring(0, index)
    } else {
      fileName
    }
  }

  def listFiles(dir: File, filter: String = ".*"): List[File] = {
    val result = collection.mutable.ListBuffer[File]()

    val files = dir.listFiles()

    for (f <- files) {
      if (f.isDirectory)
        result.appendAll(
          listFiles(f, filter)
        )
      else {
        if (f.getName.matches(filter))
          result.append(f)
      }
    }
    result.toList
  }
}

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