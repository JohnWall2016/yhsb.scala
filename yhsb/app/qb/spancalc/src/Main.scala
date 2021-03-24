import org.rogach.scallop._
import yhsb.base.command.RowRange
import yhsb.base.command.InputFile
import yhsb.base.command.SheetIndex
import yhsb.base.excel.Excel._
import yhsb.base.datetime.YearMonth
import yhsb.base.datetime.YearMonthRange
import yhsb.base.text.String.StringOps

class Conf(args: collection.Seq[String])
    extends ScallopConf(args)
    with InputFile
    with RowRange
    with SheetIndex {
  banner("缴费单年限计算程序")
  verify()
}

object Main {
  def main(args: Array[String]) = {
    val conf = new Conf(args)

    val workbook = Excel.load(conf.inputFile())
    val sheet = workbook.getSheetAt(conf.sheetIndex())
    for (index <- (conf.startRow() - 1) until conf.endRow()) {
      val row = sheet.getRow(index)
      try {
        val name = row.getCell("B").value
        val idCard = row.getCell("F").value

        println(s"${index + 1} $name $idCard")

        val birthDay = YearMonth.from(idCard.substring(6, 12).toInt)
        val totalMonths = row.getCell("H").getNumericCellValue.toInt
        val bonusMonths = row.getCell("I").getNumericCellValue.toInt

        val bought = row.getCell("N").value.split("\\|").toSeq
        //println(s"|${bought.mkString(",")}|")
        val reSpan = "^(\\d\\d\\d\\d\\d\\d)(-(\\d\\d\\d\\d\\d\\d))?$".r
        val boughtSpans = bought.flatMap { b =>
          reSpan.findFirstMatchIn(b.trim()) match {
            case None => None
            case Some(m) =>
              Some(if (m.group(3) == null) {
                YearMonthRange(
                  YearMonth.from(m.group(1).toInt),
                  YearMonth.from(m.group(1).toInt)
                )
              } else {
                YearMonthRange(
                  YearMonth.from(m.group(1).toInt),
                  YearMonth.from(m.group(3).toInt)
                )
              })
          }
        }

        val startMonth = birthDay.offset(16 * 12 + 1).max(YearMonth(2005, 8))
        val endMonth = YearMonth(2020, 7)

        val validBound = YearMonthRange(startMonth, endMonth)
        println(s"可以缴费年限: $validBound")

        println(s"已经缴费年限: ${boughtSpans.mkString(" | ")}")

        val validBounds = validBound -- boughtSpans
        val canBuyMonths = validBounds.months
        println(
          s"尚未缴费年限: ${validBounds.mkString(" | ")} | 共计: ${canBuyMonths}个月"
        )

        println("-" * 60)

        val (bonusSpan, ownBuySpan) = if (canBuyMonths >= totalMonths) {
          val offset = canBuyMonths - totalMonths
          validBounds.offset(offset).split(bonusMonths)
        } else if (canBuyMonths >= bonusMonths) {
          validBounds.split(bonusMonths)
        } else { // canBuyMonths < bonusMonths
          (validBounds, Seq())
        }

        val bonusSpanSummary =
          s"${bonusSpan.mkString(" | ")} | 共计: ${bonusSpan.months}个月"
        println(s"享受补贴年限: $bonusSpanSummary")

        val ownBuySpanSummary =
          s"${ownBuySpan.mkString(" | ")} | 共计: ${ownBuySpan.months}个月"
        println(s"个人缴费年限: $ownBuySpanSummary")
        println("-" * 80)

        row.createCell("O").setCellValue(bonusSpanSummary)
        row.createCell("P").setCellValue(ownBuySpanSummary)
      } catch {
        case ex: Exception =>
          val err = s"第 ${index + 1} 行错误: ${ex.getMessage}"
          println(err)
          println("-" * 80)

          row.createCell("O").setCellValue(err)
      }
    }
    workbook.save(conf.inputFile().insertBeforeLast(".out"))
  }
}
