package yhsb.base.text

import yhsb.base.text.String.PadMode.PadMode
import scala.util.matching.Regex

object String {
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

  object PadMode extends Enumeration {
    type PadMode = Value
    val Left, Right, Both = Value
  }

  implicit class StringOps(val s: String) extends AnyVal {
    def pad(
        width: Int,
        padChar: Char = ' ',
        specialChars: Seq[SpecialChars] = Seq(chineseChars),
        mode: PadMode = PadMode.Left
    ): String = {
      val count = padCount(s, width, specialChars)
      if (count > 0) {
        val b = new StringBuilder
        if (mode == PadMode.Left) {
          b.append(padChar.toString * count)
        } else if (mode == PadMode.Both) {
          b.append(padChar.toString * ((count + 1) / 2))
        }
        b.append(s)
        if (mode == PadMode.Right) {
          b.append(padChar.toString * count)
        } else if (mode == PadMode.Both) {
          b.append(padChar.toString * (count - ((count + 1) / 2)))
        }
        b.toString()
      } else {
        s
      }
    }

    def padLeft(
        width: Int,
        padChar: Char = ' ',
        specialChars: Seq[SpecialChars] = Seq(chineseChars)
    ) = pad(width, padChar, specialChars, PadMode.Left)

    def padRight(
        width: Int,
        padChar: Char = ' ',
        specialChars: Seq[SpecialChars] = Seq(chineseChars)
    ) = pad(width, padChar, specialChars, PadMode.Right)

    def bar(
      width: Int,
      padChar: Char = ' ',
      specialChars: Seq[SpecialChars] = Seq(chineseChars)
    ) = pad(width, padChar, specialChars, PadMode.Both)

    def insertBeforeLast(
      insert: String,
      pattern: String = "."
    ) = {
      val index = s.lastIndexOf(pattern)
      if (index >= 0) {
        s.substring(0, index) + insert + s.substring(index)
      } else {
        s + insert
      }
    }

    def replace(
      re: Regex,
      replacement: String
    ) = re.replaceAllIn(s, replacement)

    def times(t: Int) = {
      val b = new StringBuilder()
      for (_ <- 1 to t) b.append(s)
      b.toString
    }

    def isNullOrEmpty = if (s == null || s.isEmpty()) true else false

    def nonNullOrEmpty = !isNullOrEmpty
  }
}
