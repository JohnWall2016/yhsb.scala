package yhsb

import utest.{TestSuite, Tests, test}
import yhsb.base.datetime.{YearMonth, YearMonthRange}

object DateTimeTest extends TestSuite {
  def tests =
    Tests {
      test("month") {
        val birthDay = YearMonth(1990, 12) //??
        val boughtSpans = Seq( //??
          YearMonthRange(YearMonth(2008, 1), YearMonth(2009, 9)),
          YearMonthRange(YearMonth(2010, 2), YearMonth(2010, 4))
        )
        val totalMonths = 180 //??
        val bonusMonths = 48 //??

        val startMonth = birthDay.offset(16 * 12 + 1).max(YearMonth(2005, 8)) //?
        val endMonth = YearMonth(2020, 7) //?

        val validBound = YearMonthRange(startMonth, endMonth)
        println(s"可以缴费年限: $validBound")

        println(s"已经缴费年限: ${boughtSpans.mkString(" | ")}")

        val validBounds = validBound -- boughtSpans
        val canBuyMonths = validBounds.months
        println(
          s"尚未缴费年限: ${validBounds.mkString(" | ")} | 共计: ${canBuyMonths}个月"
        )

        println("-" * 80)

        val (bonusSpan, ownBuySpan) = if (canBuyMonths >= totalMonths) {
          val offset = canBuyMonths - totalMonths
          validBounds.offset(offset).split(bonusMonths)
        } else if (canBuyMonths >= bonusMonths) {
          validBounds.split(bonusMonths)
        } else { // canBuyMonths < bonusMonths
          (validBounds, Seq())
        }
        println(
          s"享受补贴年限: ${bonusSpan.mkString(" | ")} | 共计: ${bonusSpan.months}个月"
        )
        println(
          s"个人缴费年限: ${ownBuySpan.mkString(" | ")} | 共计: ${ownBuySpan.months}个月"
        )

        println("-" * 80)
      }
    }
}
