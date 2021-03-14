package yhsb.base.text

object Strings {
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
    width - s.foldLeft(0) { (count, char) =>
      count + specialChars
        .find(sp => sp.range.contains(char))
        .map(_.width)
        .getOrElse(1)
    }
  }

  implicit class StringOps(val s: String) extends AnyVal {
    def pad(
        width: Int,
        padChar: Char = ' ',
        specialChars: Seq[SpecialChars] = Seq(chineseChars),
        left: Boolean = false
    ): String = {
      val count = padCount(s, width, specialChars)
      if (count > 0) {
        val b = new StringBuilder
        if (left) b.append(padChar.toString * count)
        b.append(s)
        if (!left) b.append(padChar.toString * count)
        b.toString()
      } else {
        s
      }
    }

    def padLeft(
        width: Int,
        padChar: Char = ' ',
        specialChars: Seq[SpecialChars] = Seq(chineseChars)
    ) = s.pad(width, padChar, specialChars, true)

    def padRight(
        width: Int,
        padChar: Char = ' ',
        specialChars: Seq[SpecialChars] = Seq(chineseChars)
    ) = s.pad(width, padChar, specialChars, false)
  }
}
