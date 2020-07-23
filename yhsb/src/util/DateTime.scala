package yhsb.util

import scala.util.matching.Regex
import java.{util => ju}
import java.text.SimpleDateFormat
import java.util.Date
import scala.collection.mutable

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

case class Month(year: Int, month: Int) extends Ordering[Month] {
  override def compare(x: Month, y: Month): Int = {
    val yearDelta = x.year - y.year
    if (yearDelta < 0) -1
    else if (yearDelta > 0) 1
    else {
      val monthDelta = x.month - y.month
      if (monthDelta < 0) -1
      else if (monthDelta > 0) 1
      else 0
    }
  }
}

case class MonthRange(start: Month, end: Month) {
  def -(that: MonthRange): Option[Seq[Month]] = {
    ???
  }
}
