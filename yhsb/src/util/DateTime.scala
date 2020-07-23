package yhsb.util

import scala.util.matching.Regex
import java.{util => ju}
import java.text.SimpleDateFormat
import java.util.Date

object DateTime {
  def toDashedDate(
      date: String,
      format: Regex = "^(\\d\\d\\d\\d)(\\d\\d)(\\d\\d)$".r
  ): String =
    format.findFirstMatchIn(date) match {
      case Some(m) => m.group(1) + "-" + m.group(2) + "-" + m.group(3)
      case None =>
        throw new IllegalArgumentException("Invalid date format (YYYYMMDD)")
    }

  def formatDate(
      date: Date = new Date(),
      format: String = "yyyyMMdd"
  ): String = new SimpleDateFormat(format).format(date)
}
