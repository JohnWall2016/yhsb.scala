package yhsb.util

import java.nio.file.Path

object Files {
  def appendToFileName(fileName: String, appendString: String) = {
    val index = fileName.lastIndexOf(".")
    if (index >= 0) {
      fileName.substring(0, index) + appendString + fileName.substring(index)
    } else {
      fileName + appendString
    }
  }

  def appendToFileName(fileName: Path, appendString: String): String =
    appendToFileName(fileName.toString(), appendString)

  def trimExtension(fileName: String) = {
    val index = fileName.lastIndexOf(".")
    if (index >= 0) {
      fileName.substring(0, index)
    } else {
      fileName
    }
  }
}
