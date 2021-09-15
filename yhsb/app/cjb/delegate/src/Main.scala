package yhsb.app.cjb

import yhsb.base.command.Command
import yhsb.base.command.Subcommand
import yhsb.base.excel.Excel._
import yhsb.base.datetime.Formatter
import yhsb.base.text.String.StringOps
import yhsb.base.util._
import yhsb.cjb.net.Session
import yhsb.cjb.net.protocol.DelegatePersonQuery
import yhsb.cjb.net.protocol.{DFType, DFPayType}

import scala.collection.mutable
import java.math.{BigDecimal => JBigDecimal}
import yhsb.cjb.net.protocol.DelegatePaymentQuery
import yhsb.cjb.net.protocol.DelegatePaymentDetailQuery
import yhsb.cjb.net.protocol.DelegatePaymentPersonalDetailQuery
import java.text.Collator
import java.util.Locale
import yhsb.cjb.net.protocol.DFState
import yhsb.cjb.net.protocol.CBState
import yhsb.cjb.net.protocol.BankInfoQuery

class Delegate(args: collection.Seq[String]) extends Command(args) {
  banner("代发数据导出制表程序")
  addSubCommand(new PersonList)
  addSubCommand(new PaymentList)
}

class PersonList extends Subcommand("personList") {
  banner("正常代发人员名单导出")

  val dfType = trailArg[String](
    descr = "代发类型: 801 - 独生子女, 802 - 乡村教师, 803 - 乡村医生, 807 - 电影放映员, 808 - 核工业"
  )

  val exportAll = opt[Boolean](
    descr = "导出所有居保正常代发人员",
    name = "all",
    short = 'a',
    default = Some(false)
  )

  val template = """D:\代发管理\雨湖区城乡居民基本养老保险代发人员名单.xlsx"""

  override def execute(): Unit = {
    println("开始导出数据")

    val workbook = Excel.load(template)
    val sheet = workbook.getSheetAt(0)

    val startRow = 3
    var currentRow = startRow

    var payedSum = BigDecimal(0)

    val date = Formatter.formatDate("yyyyMMdd")
    val dateCH = Formatter.formatDate("yyyy年M月d日")
    sheet("G2").value = s"制表时间：$dateCH"

    Session.use() { session =>
      session
        .request(DelegatePersonQuery(dfType(), "1", ""))
        .foreach { it =>
          if (it.valid) {
            println(s"${it.name.padRight(8)}${it.idCard}")

            if (
              it.dfState == DFState.Normal ||
              (exportAll() && it.dfState == DFState.Paused && it.cbState == CBState.Normal)
            ) {
              if (
                !(dfType() == "801" && it.totalPayed == BigDecimal(5000))
              ) {
                val row = sheet.getOrCopyRow(currentRow, startRow)
                currentRow += 1
                row("A").value = currentRow - startRow
                row("B").value = it.csName
                row("C").value = it.name
                row("D").value = it.idCard
                row("E").value = it.startYearMonth
                row("F").value = it.standard
                row("G").value = it.dfType
                row("H").value = it.dfState.toString()
                row("I").value = it.cbState.toString()
                if (it.endYearMonth != 0) {
                  row("J").value = it.endYearMonth
                }
                row("K").value = it.totalPayed

                val bankResult = session.request(BankInfoQuery(it.idCard))
                if (bankResult.isEmpty) {
                  row("L").value = "否"
                } else {
                  row("L").value = "是"
                }

                if (it.cbState == CBState.Paused) {
                  row("M").value = "居保是暂停状态，是否停发代发"
                } else if (it.dfState == DFState.Paused && it.cbState == CBState.Normal) {
                  row("M").value = "居保是正常状态，是否恢复代发"
                }

                payedSum += it.totalPayed ?: BigDecimal(0)
              }
            }
          }
        }
    }
    if (currentRow > startRow) {
      val row = sheet.getOrCopyRow(currentRow, startRow)
      row("C").value = "共计"
      row("D").value = currentRow - startRow
      row("J").value = "合计"
      row("K").value = payedSum

      val path = template.insertBeforeLast(
        s"(${DFType(dfType())})${if (exportAll()) "ALL" else ""}$date"
      )

      println(s"保存: $path")
      workbook.save(path)
    } else {
      println("无数据")
    }

    println("结束数据导出")
  }
}

class PaymentList extends Subcommand("paymentList") {
  banner("代发支付明细导出")

  val dfPayType = trailArg[String](
    descr =
      "业务类型: DF0001 - 独生子女, DF0002 - 乡村教师, DF0003 - 乡村医生, DF0007 - 电影放映员, DF0008 - 核工业"
  )

  val yearMonth = trailArg[String](
    descr = "支付年月: 格式 YYYYMM, 如 201901"
  )

  val template = """D:\代发管理\雨湖区城乡居民基本养老保险代发人员支付明细.xlsx"""

  case class Item(
      csName: String,
      name: String,
      idCard: String,
      payType: String,
      yearMonth: Int,
      startDate: Option[Int],
      endDate: Option[Int],
      amount: BigDecimal,
      memo: String
  )

  override def execute(): Unit = {
    println("开始导出数据")

    var items = mutable.ListBuffer[Item]()
    var total = BigDecimal(0)
    val payType = DFPayType(dfPayType())

    Session.use("006") { session =>
      session
        .request(DelegatePaymentQuery(dfPayType(), yearMonth()))
        .foreach { list =>
          if (list.typeCh != null) {
            session
              .request(DelegatePaymentDetailQuery(list.payList))
              .foreach { detail =>
                if (detail.csName != null && detail.flag == "0") {
                  val (startDate, endDate) =
                    session
                      .request(DelegatePaymentPersonalDetailQuery(detail))
                      .let { personDetail =>
                        val count = personDetail.size
                        if (count > 0) {
                          (
                            Some(personDetail.head.date),
                            if (count > 2) {
                              Some(personDetail.apply(count - 2).date)
                            } else {
                              Some(personDetail.head.date)
                            }
                          )
                        } else {
                          (None, None)
                        }
                      }
                  total += detail.amount

                  items.addOne(
                    Item(
                      detail.csName,
                      detail.name,
                      detail.idCard,
                      payType.toString(),
                      detail.yearMonth,
                      startDate,
                      endDate,
                      detail.amount,
                      if (list.bankType != null) ""
                      else "未绑定支付账户"
                    )
                  )
                }
              }
          }
        }
    }

    if (items.nonEmpty) {
      items = items.sortWith { (e1, e2) =>
        Collator
          .getInstance(Locale.CHINESE)
          .compare(e1.csName, e2.csName) < 0
      }

      val workbook = Excel.load(template)
      val sheet = workbook.getSheetAt(0)
      val startRow = 3
      var currentRow = startRow
      val date = Formatter.formatDate("yyyyMMdd")
      val dateCH = Formatter.formatDate("yyyy年M月d日")

      sheet("G2").value = s"制表时间：$dateCH"

      items.foreach { item =>
        val row = sheet.getOrCopyRow(currentRow, startRow)
        currentRow += 1
        row("A").value = currentRow - startRow
        row("B").value = item.csName
        row("C").value = item.name
        row("D").value = item.idCard
        row("E").value = item.payType
        row("F").value = item.yearMonth
        row("G").value = item.startDate
        row("H").value = item.endDate
        row("I").value = item.amount
        row("J").value = item.memo
      }

      val row = sheet.getOrCopyRow(currentRow, startRow)
      row("C").value = "共计"
      row("D").value = currentRow - startRow
      row("H").value = "合计"
      row("I").value = total

      val path = template.insertBeforeLast(s"($payType)$date")
      println(s"保存: $path")
      workbook.save(path)
    } else {
      println("无数据")
    }

    println("结束数据导出")
  }
}

object Main {
  def main(args: Array[String]) = new Delegate(args).runCommand()
}
