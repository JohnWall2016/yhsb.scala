import yhsb.base.command.Command
import yhsb.base.excel.Excel._
import yhsb.base.datetime.Formatter
import yhsb.base.math.Number.BigDecimalOps
import yhsb.base.text.String.StringOps
import yhsb.cjb.net.Session
import yhsb.cjb.net.protocol.PaymentQuery
import yhsb.cjb.net.protocol.PaymentPersonalDetailQuery
import yhsb.cjb.net.protocol.SessionOps.CeaseInfo
import yhsb.cjb.net.protocol.PayState

class Payment(args: collection.Seq[String]) extends Command(args) {
  banner("财务支付单生成程序")

  val yearMonth = trailArg[String](
    descr = "发放年月: 格式 YYYYMM, 如 201901"
  )

  val state = trailArg(
    descr = "业务状态: 0-待支付, 1-已支付, 默认为：所有",
    required = false,
    default = Some("")
  )

  val template = """D:\支付管理\雨湖区居保个人账户返还表.xlsx"""

  override def execute(): Unit = {
    val workbook = Excel.load(template)
    val sheet = workbook.getSheetAt(0)

    val (year, month, _) = Formatter.splitDate(yearMonth())
    sheet("A1").value = s"${year}年${month.stripPrefix("0")}月个人账户返还表"

    val date = Formatter.formatDate()
    val dateCH = Formatter.formatDate("yyyy年M月d日")
    sheet("H2").value = s"制表时间：$dateCH"

    Session.use() { session =>
      val startRow = 4
      var currentRow = startRow
      var sum: BigDecimal = 0

      val items =
        session
          .request(PaymentQuery(yearMonth(), PayState.Val(state())))
          .sortWith(_.payList < _.payList)

      items.foreach { item =>
        if (item.objectType == "3") { // 个人支付
          val it = session
            .request(PaymentPersonalDetailQuery(item))
            .head
          val info = session.getStopInfoByIdCard(it.idCard, true)

          val row = sheet.getOrCopyRow(currentRow, startRow)
          currentRow += 1

          row("A").value = currentRow - startRow
          row("B").value = it.name
          row("C").value = it.idCard
          row("D").value = 
            info.map(v => s"${it.payType}(${v.reason})")
              .getOrElse(it.payType.toString())
          row("E").value = it.payList
          row("F").value = it.amount
          row("G").value = BigDecimal(it.amount).toChineseMoney
          row("H").value = item.name
          row("I").value = item.account
          row("J").value = info.flatMap(_.bankName)

          sum += it.amount
        }
      }

      if (currentRow > startRow) {
        val row = sheet.getOrCopyRow(currentRow, startRow)
        row("A").value = "合计"
        row("F").value = sum

        workbook.saveAfter(
          template.insertBeforeLast(date)
        ) { path =>
          println(s"保存: $path")
        }
      }
    }
  }
}

object Main {
  def main(args: Array[String]) = new Payment(args).runCommand()
}
