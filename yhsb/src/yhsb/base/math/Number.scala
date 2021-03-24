package yhsb.base.math

import scala.math.BigDecimal.RoundingMode

object Number {
  val chineseNumbers = List(
    "零", "壹", "贰", "叁", "肆",
    "伍", "陆", "柒", "捌", "玖",
  )

  val chinesePlaces = List(
    "", "拾", "佰", "仟", "万", "亿",
  )

  val chineseUnits = List(
    "分", "角", "元",
  )

  val chineseWhole = "整"

  implicit class BigDecimalOps(d: BigDecimal) {
    def toChineseMoney: String = {
      val number = (round(2) * 100).toBigInt
      var integer = number / 100
      val fraction = number % 100

      val length = integer.toString.length
      var result = ""
      var previousZero = false
      var break = false

      for (i <- (length - 1) to 0 by -1 if !break) {
        val base = BigInt(10) ** i
        val quot = integer / base
        if (quot > 0) {
          if (previousZero) result += chineseNumbers(0)
          result += chineseNumbers(quot.toInt) + chinesePlaces(i % 4)
          previousZero = false
        } else if (quot == 0 && result.nonEmpty) {
          previousZero = true
        }
        if (i >= 4) {
          if (i % 8 == 0 && result.nonEmpty) {
            result += chinesePlaces(5)
          } else if (i % 4 == 0 && result.nonEmpty) {
            result += chinesePlaces(4)
          }
        }
        integer %= base
        if (integer == 0 && i != 0) {
          previousZero = true
          break = true
        }
      }
      result += chineseUnits(2)

      if (fraction == 0) {
        result += chineseWhole
      } else {
        val quot = fraction / 10
        val rem = fraction % 10
        if (rem == 0) {
          if (previousZero) result += chineseNumbers(0)
          result += chineseNumbers(quot.toInt) + chineseUnits(1) + chineseWhole
        } else {
          if (previousZero || quot == 0) {
            result += chineseNumbers(0)
          }
          if (quot != 0) {
            result += chineseNumbers(quot.toInt) + chineseUnits(1)
          }
          result += chineseNumbers(rem.toInt) + chineseUnits(0)
        }
      }
      result
    }

    def round(precision: Int) = d.setScale(precision, RoundingMode.HALF_UP)
  }

  implicit class BigIntOps(i: BigInt) {
    def **(e: Int) = i.pow(e)
  }
}