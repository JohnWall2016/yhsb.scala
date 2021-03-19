package yhsb.base.io

import java.io.File
import java.nio.file.Path
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

object PathOps {
  implicit class StringEx(path: String) {
    def /(rest: String) = Paths.get(path, rest)
    def /(rest: Path) = Paths.get(path, rest.toString)
  }

  implicit class PathEx(path: Path) {
    def /(rest: String) = path.resolve(rest)
    def /(rest: Path) = path.resolve(rest)
  }

  implicit def toPath(path: String) = Paths.get(path)
}