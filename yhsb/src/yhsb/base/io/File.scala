package yhsb.base.io

import java.io.{File => JFile}

object File {
  def trimExtension(fileName: String) = {
    val index = fileName.lastIndexOf(".")
    if (index >= 0) {
      fileName.substring(0, index)
    } else {
      fileName
    }
  }

  def listFiles(
      dir: JFile,
      filter: String = ".*",
      recursive: Boolean = false
  ): List[JFile] = {
    val result = collection.mutable.ListBuffer[JFile]()

    val files = dir.listFiles()

    for (f <- files) {
      if (recursive && f.isDirectory) {
        result.appendAll(
          listFiles(f, filter, recursive)
        )
      } else {
        if (f.getName.matches(filter))
          result.append(f)
      }
    }
    result.toList
  }
}
