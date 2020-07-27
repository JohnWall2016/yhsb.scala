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

  case class CharRange(start: Char, end: Char) {
    if (start > end)
      throw new IllegalArgumentException("start must be <= end")

    def contains(ch: Char) = ch >= start && ch <= end
  }

  case class SpecialChars(range: CharRange, width: Int)

  private val chineseChars = SpecialChars(
    CharRange('\u4e00', '\u9fa5'),
    2
  )

  private def padCount(
      s: String,
      width: Int,
      specialChars: Seq[SpecialChars]
  ): Int = {
    s.foldLeft(0) { (count, char) =>
      count + specialChars
        .find(sp => sp.range.contains(char))
        .map(_.width)
        .getOrElse(1)
    }
  }

  implicit class StringOps(val s: String) extends AnyVal {
    def padRight(
        width: Int,
        padChar: Char = ' ',
        specialChars: Seq[SpecialChars] = Seq(chineseChars)
    ): String = {
      val count = padCount(s, width, specialChars)
      if (count > 0) {
        val b = new StringBuilder
        b.append(s)
        b.append(padChar * count)
        b.toString()
      } else {
        s
      }
    }
  }
}
