package yhsb.util.datetime

import scala.util.matching.Regex
import java.{util => ju}
import java.text.SimpleDateFormat
import java.util.Date
import scala.collection.mutable

object formater {
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

case class Month(year: Int, month: Int) {
  if (month < 1 || month > 12)
    throw new IllegalArgumentException("month must be >= 1 and <= 12")

  def offset(months: Int): Month = {
    val ms = month + months
    var y = ms / 12
    var m = ms % 12
    if (m <= 0) {
      y -= 1
      m += 12
    }
    Month(year + y, m)
  }

  override def toString() = f"$year%04d$month%02d"
}

object Month {
  def from(m: Int): Month = Month(m / 100, m % 100)
}

object MonthOrdering extends Ordering[Month] {
  override def compare(x: Month, y: Month): Int =
    (x.year * 12 + x.month) - (y.year * 12 + y.month)
}

import MonthOrdering.mkOrderingOps

case class MonthRange(start: Month, end: Month) {
  if (start > end) throw new IllegalArgumentException("start must be <= end")

  def -(that: MonthRange): Seq[MonthRange] = {
    if (that.end < this.start || this.end < that.start) {
      Seq(this)
    } else if (that.start <= this.start) {
      if (that.end < this.end) {
        Seq(MonthRange(that.end.offset(1), this.end))
      } else {
        Seq()
      }
    } else { // that.start > this.start
      if (that.end < this.end) {
        Seq(
          MonthRange(this.start, that.start.offset(-1)),
          MonthRange(that.end.offset(1), this.end)
        )
      } else {
        Seq(MonthRange(this.start, that.start.offset(-1)))
      }
    }
  }

  def --(those: Seq[MonthRange]): Seq[MonthRange] =
    sub(mutable.ListBuffer(this), those)

  private def sub(
      these: mutable.ListBuffer[MonthRange],
      those: Seq[MonthRange]
  ): Seq[MonthRange] = {
    those.foreach { that =>
      these.flatMapInPlace(_ - that)
    }
    these.result()
  }

  def months = {
    (end.year * 12 + end.month) - (start.year * 12 + start.month) + 1
  }

  override def toString() = s"$start-$end"
}

object MonthRange {
  implicit class MonthRangeSeq(val seq: Seq[MonthRange]) extends AnyVal {
    def months = seq.foldLeft(0)(_ + _.months)

    def offset(months: Int): Seq[MonthRange] = {
      seq match {
        case Nil => Seq()
        case head :: next => {
          val mths = head.months
          if (mths > months) {
            MonthRange(head.start.offset(months), head.end) :: next
          } else if (mths == months) {
            next
          } else {
            next.offset(months - mths)
          }
        }
      }
    }

    def split(months: Int): (Seq[MonthRange], Seq[MonthRange]) = {
      seq match {
        case Nil => (Seq(), Seq())
        case head :: next => {
          val mths = head.months
          if (mths > months) {
            (
              MonthRange(head.start, head.start.offset(months - 1)) :: Nil,
              MonthRange(head.start.offset(months), head.end) :: next
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
}
