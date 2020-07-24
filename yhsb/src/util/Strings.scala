package yhsb.util

object Strings {
  def appendToFileName(fileName: String, appendString: String) = {
    val index = fileName.lastIndexOf(".")
    if (index >= 0) {
      fileName.substring(0, index) + appendString + fileName.substring(index)
    } else {
      fileName + appendString
    }
  }
}
