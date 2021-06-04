package yhsb.base.datetime

import scala.util.matching.Regex
import java.{util => ju}
import java.text.SimpleDateFormat
import java.util.Date
import scala.collection.mutable

object Formatter {
  def toDashedDate(
      date: String,
      format: String = """^(\d\d\d\d)(\d\d)(\d\d)$"""
  ): String =
    format.r.findFirstMatchIn(date) match {
      case Some(m) => m.group(1) + "-" + m.group(2) + "-" + m.group(3)
      case None =>
        throw new IllegalArgumentException(
          s"Invalid date format ($format): $date"
        )
    }

  def formatDate(
      format: String = "yyyyMMdd",
      date: Date = new Date()
  ): String = new SimpleDateFormat(format).format(date)

  def splitDate(
      date: String,
      format: String = """^(\d\d\d\d)(\d\d)(\d\d)?"""
  ): (String, String, String) =
    format.r.findFirstMatchIn(date) match {
      case Some(m) => (m.group(1), m.group(2), m.group(3))
      case None =>
        throw new IllegalArgumentException(
          s"Invalid date format ($format): $date"
        )
    }

  def normalizeSpan(startTime: String, endTime: String, to: String = "-") = {
    val (sy, sm, _) = splitDate(startTime)
    val (ey, em, _) = splitDate(endTime)
    if (sy == ey) {
      if (sm == em) {
        s"${sy}年${sm.stripPrefix("0")}月"
      } else {
        s"${sy}年${sm.stripPrefix("0")}$to${em.stripPrefix("0")}月"
      }
    } else {
      s"${sy}年${sm.stripPrefix("0")}月$to${ey}年${em.stripPrefix("0")}月"
    }
  }
}

case class YearMonth(year: Int, month: Int) {
  require(
    year > 0 && (month >= 1 && month <= 12),
    s"year must be greater than 0 and month must be between 1 and 12: $year $month"
  )

  def offset(months: Int): YearMonth = {
    val ms = month + months
    var y = ms / 12
    var m = ms % 12
    if (m <= 0) {
      y -= 1
      m += 12
    }
    YearMonth(year + y, m)
  }

  def toYearMonth = year * 100 + month

  override def toString = f"$year%04d$month%02d"
}

object YearMonth extends Ordering[YearMonth] {
  def from(m: Int): YearMonth = YearMonth(m / 100, m % 100)

  override def compare(x: YearMonth, y: YearMonth): Int =
    (x.year * 12 + x.month) - (y.year * 12 + y.month)
}

/**
 * A range of yearmonths
 *
 * @param start start yearmonth of the range
 * @param end end yearmonth of the range which is inclusive
 */
case class YearMonthRange(start: YearMonth, end: YearMonth)
  extends Iterable[YearMonth] {
  require(
    start <= end,
    s"start must be less than or equal to end: $start, $end"
  )

  def -(that: YearMonthRange): Seq[YearMonthRange] = {
    if (that.end < this.start || this.end < that.start) {
      Seq(this)
    } else if (that.start <= this.start) {
      if (that.end < this.end) {
        Seq(YearMonthRange(that.end.offset(1), this.end))
      } else {
        Seq()
      }
    } else { // that.start > this.start
      if (that.end < this.end) {
        Seq(
          YearMonthRange(this.start, that.start.offset(-1)),
          YearMonthRange(that.end.offset(1), this.end)
        )
      } else {
        Seq(YearMonthRange(this.start, that.start.offset(-1)))
      }
    }
  }

  def --(those: Seq[YearMonthRange]): Seq[YearMonthRange] =
    sub(mutable.ListBuffer(this), those)

  private def sub(
      these: mutable.ListBuffer[YearMonthRange],
      those: Seq[YearMonthRange]
  ): Seq[YearMonthRange] = {
    those.foreach { that =>
      these.flatMapInPlace(_ - that)
    }
    these.result()
  }

  def months = {
    (end.year * 12 + end.month) - (start.year * 12 + start.month) + 1
  }

  override def toString = s"$start-$end"

  override def iterator: Iterator[YearMonth] = {
    new Iterator[YearMonth]() {
      private var cur = YearMonth.from(start.toYearMonth)
      private val last = YearMonth.from(end.toYearMonth)

      def hasNext: Boolean = cur <= last

      def next(): YearMonth = {
        val ret = YearMonth.from(cur.toYearMonth)
        cur = cur.offset(1)
        ret
      }
    }
  }
}

object YearMonthRange {
  implicit class MonthRangeSeq(val seq: Seq[YearMonthRange]) extends AnyVal {
    def months = seq.foldLeft(0)(_ + _.months)

    def offset(months: Int): Seq[YearMonthRange] = {
      seq match {
        case Nil => Seq()
        case head :: next =>
          val mths = head.months
          if (mths > months) {
            YearMonthRange(head.start.offset(months), head.end) :: next
          } else if (mths == months) {
            next
          } else {
            next.offset(months - mths)
          }
      }
    }

    def split(months: Int): (Seq[YearMonthRange], Seq[YearMonthRange]) = {
      seq match {
        case Nil => (Seq(), Seq())
        case head :: next =>
          val mths = head.months
          if (mths > months) {
            (
              YearMonthRange(head.start, head.start.offset(months - 1)) :: Nil,
              YearMonthRange(head.start.offset(months), head.end) :: next
            )
          } else if (mths == months) {
            (head :: Nil, next)
          } else {
            val (h, n) = next.split(months - mths)
            (head :: h.toList, n)
          }
      }
    }
  }
}
