package yhsb

import utest.{TestSuite, Tests, test}
import yhsb.base.datetime.{Month, MonthRange}

object DateTimeTest extends TestSuite {
  def tests =
    Tests {
      test("month") {
        import yhsb.base.datetime.MonthOrdering.mkOrderingOps

        val birthDay = Month(1990, 12) //??
        val boughtSpans = Seq( //??
          MonthRange(Month(2008, 1), Month(2009, 9)),
          MonthRange(Month(2010, 2), Month(2010, 4))
        )
        val totalMonths = 180 //??
        val bonusMonths = 48 //??

        val startMonth = birthDay.offset(16 * 12 + 1).max(Month(2005, 8)) //?
        val endMonth = Month(2020, 7) //?

        val validBound = MonthRange(startMonth, endMonth)
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
