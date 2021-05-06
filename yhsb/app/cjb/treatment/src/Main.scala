import java.nio.file.Files
import java.nio.file.Path
import java.text.Collator
import java.util.Locale

import org.rogach.scallop.ScallopConf

import scala.collection.mutable.LinkedHashMap

import yhsb.base.command.Command
import yhsb.base.command.Subcommand
import yhsb.base.excel.Excel._
import yhsb.base.util._
import yhsb.base.io.Path._
import yhsb.base.command.RowRange
import yhsb.base.datetime.Formatter
import yhsb.base.text.String.StringOps
import yhsb.cjb.net.Session
import yhsb.cjb.net.protocol._
import yhsb.cjb.net.protocol.Division.GroupOps

object Main {
  def main(args: Array[String]): Unit = new Treatment(args).runCommand()
}

class Treatment(args: collection.Seq[String]) extends Command(args) {
  banner("信息核对报告表、养老金计算表生成、待遇认证表格等相关程序")
  addSubCommand(new Download)
  addSubCommand(new Split)
  addSubCommand(new PayFailedList)
  addSubCommand(new UncertPauseList)
  addSubCommand(new UncertList)
  addSubCommand(new ArrearList)
}

trait ReportDate { _: ScallopConf =>
  val date = trailArg[String](
    descr = "报表生成日期, 格式: YYYYMMDD, 例如: 20210101"
  )
}

class Download extends Subcommand("download") with ReportDate {
  descr("从业务系统下载信息核对报告表")

  val outputDir = """D:\待遇核定"""

  val template = "信息核对报告表模板.xlsx"

  override def execute(): Unit = {
    val result = Session.use() { session =>
      session.request(TreatmentReviewQuery(reviewState = "0"))
    }

    val workbook = Excel.load(outputDir / template)
    val sheet = workbook.getSheetAt(0)
    val startRow = 3
    var currentRow = startRow

    result.foreach { it =>
      val index = currentRow - startRow + 1

      println(s"$index ${it.idCard} ${it.name} ${it.memo ?: ""}")

      val row = sheet.getOrCopyRow(currentRow, startRow, false)
      currentRow += 1
      row("A").value = index
      row("B").value = it.name
      row("C").value = it.idCard
      row("D").value = it.division
      row("E").value = it.payAmount
      row("F").value = it.payMonth
      row("L").value = it.memo
    }

    workbook.saveIf(currentRow > startRow)(
      outputDir / s"信息核对报告表${date()}.xlsx",
      p => println(s"保存: $p"),
      _ => println("无数据")
    )
  }
}

class Split extends Subcommand("split") with ReportDate with RowRange {
  descr("对下载的信息表分组并生成养老金计算表")

  val outputDir = """D:\待遇核定"""

  val template = "养老金计算表模板.xlsx"

  override def execute(): Unit = {
    val (year, month, _) = Formatter.splitDate(date())

    val inputExcel = outputDir / s"信息核对报告表${date()}.xlsx"
    val infoExcel = outputDir / "信息核对报告表模板.xlsx"
    val destDir = outputDir / s"${year}年${month.stripPrefix("0")}月待遇核定数据"

    val workbook = Excel.load(inputExcel)
    val sheet = workbook.getSheetAt(0)

    println("生成分组映射表")
    val map = (for (index <- (startRow() - 1) until endRow())
      yield {
        (sheet.getRow(index)("D").value, index)
      }).groupByDwAndCsName()

    println("生成分组目录并分别生成信息核对报告表")
    if (Files.exists(destDir)) {
      Files.move(destDir, s"${destDir.toString}.orig")
    }
    Files.createDirectory(destDir)

    for ((dw, csMap) <- map) {
      println(s"$dw:")
      Files.createDirectory(destDir / dw)

      for ((cs, indexes) <- csMap) {
        println(s"  $cs: ${indexes.mkString(",")}")
        Files.createDirectory(destDir / dw / cs)

        val outWorkbook = Excel.load(infoExcel)
        val outSheet = outWorkbook.getSheetAt(0)
        val startRow = 3
        var currentRow = startRow

        indexes.foreach { rowIndex =>
          val index = currentRow - startRow + 1
          val inRow = sheet.getRow(rowIndex)

          println(s"    $index ${inRow("C").value} ${inRow("B").value}")

          val outRow = outSheet.getOrCopyRow(currentRow, startRow)
          currentRow += 1
          outRow("A").value = index
          inRow.copyTo(outRow, "B", "C", "D", "E", "F", "G", "H", "I", "J", "L")
        }

        outWorkbook.save(destDir / dw / cs / s"${cs}信息核对报告表.xlsx")
      }
    }

    println("\n按分组生成养老金养老金计算表")
    Session.use() { session =>
      for ((dw, csMap) <- map) {
        for ((cs, indexes) <- csMap) {
          indexes.foreach { index =>
            val row = sheet.getRow(index)
            val name = row("B").value
            val idCard = row("C").value
            println(s"  $idCard $name")
            
            downloadPaymentInfoReport(
              session,
              name,
              idCard,
              destDir / dw / cs
            )
          }
        }
      }
    }
  }

  private def downloadPaymentInfoReport(
      session: Session,
      name: String,
      idCard: String,
      outDir: Path,
      retry: Int = 3
  ) = {
    var times = 0; var success = false
    val result = session.request(TreatmentReviewQuery(idCard, "0"))
    val bankResult = session.request(BankInfoQuery(idCard))
    if (result.nonEmpty) {
      while (!success && times < retry) {
        times += 1
        result.head.getTreatmentInfoMatch() match {
          case None =>
          case Some(m) =>
            success = true
            val workbook = Excel.load(outputDir / template)
            val sheet = workbook.getSheetAt(0)
            sheet("A5").value = m.group(1)
            sheet("B5").value = m.group(2)
            sheet("C5").value = m.group(3)
            sheet("F5").value = m.group(4)
            sheet("I5").value = m.group(5)
            sheet("L5").value = m.group(6)
            sheet("A8").value = m.group(7)
            sheet("B8").value = m.group(8)
            sheet("C8").value = m.group(9)
            sheet("E8").value = m.group(10)
            sheet("F8").value = m.group(11)
            sheet("G8").value = m.group(12)
            sheet("H8").value = m.group(13)
            sheet("I8").value = m.group(14)
            sheet("J8").value = m.group(15)
            sheet("K8").value = m.group(16)
            sheet("L8").value = m.group(17)
            sheet("M8").value = m.group(18)
            sheet("N8").value = m.group(19)
            sheet("A11").value = m.group(20)
            sheet("B11").value = m.group(21)
            sheet("C11").value = m.group(22)
            sheet("D11").value = m.group(23)
            sheet("E11").value = m.group(24)
            sheet("F11").value = m.group(25)
            sheet("G11").value = m.group(26)
            sheet("H11").value = m.group(27)
            sheet("I11").value = m.group(28)
            sheet("J11").value = m.group(29)
            sheet("K11").value = m.group(30)
            sheet("L11").value = m.group(31)
            sheet("M11").value = m.group(32)
            sheet("N11").value = m.group(33)
            sheet("I12").value = Formatter.formatDate("yyyy-MM-dd HH:mm:ss")

            if (bankResult.nonEmpty) {
              bankResult.head.let { it =>
                sheet("B15").value = it.countName
                sheet("F15").value = it.bankType.toString()

                var card = it.cardNumber
                val len = card.length
                if (len > 7) {
                  card =
                    card.substring(0, 3) + "*".times(len - 7) + card.substring(
                      len - 4
                    )
                } else if (len > 4) {
                  card = "*".times(len - 4) + card.substring(len - 4)
                }
                sheet("J15").value = card
              }
            } else {
              sheet("B15").value = "未绑定银行账户"
            }
            workbook.save(outDir / s"${name}[$idCard]养老金计算表.xlsx")
        }
      }
    }
    if (result.isEmpty || !success) {
      throw new Exception("未查到该人员核定数据")
    }
  }
}

class PayFailedList extends Subcommand("failList") {
  descr("从业务系统下载支付失败人员名单")

  val yearMonth = trailArg[String](
    descr = "支付年月, 格式: YYYYMM, 例如: 202101"
  )

  val outputDir = """D:\待遇核定"""
  val template = "待遇支付失败人员名单模板.xls"

  override def execute(): Unit = {
    val (year, month, _) = Formatter.splitDate(yearMonth())

    val items = Session.use() { session =>
      session
        .request(PaymentQuery(yearMonth(), PayState.Wait))
        .filter(_.objectType == "1")
        .flatMap(it => session.request(PaymentPersonalDetailQuery(it)))
        .filter(_.idCard.nonNullAndEmpty)
        .sortWith((e1, e2) =>
          Collator
            .getInstance(Locale.CHINESE)
            .compare(e1.csName, e2.csName) < 0
        )
    }
    if (items.nonEmpty) {
      val workbook = Excel.load(outputDir / template)
      val sheet = workbook.getSheetAt(0)
      val startRow = 1
      var currentRow = startRow

      println("开始导出数据")

      items.zipWithIndex.foreach { case (item, index) =>
        println(
          s"${index + 1} ${item.name.padRight(6)} ${item.idCard} ${item.csName}"
        )
        val row = sheet.getOrCopyRow(currentRow, startRow)
        currentRow += 1
        row("A").value = item.csName
        row("B").value = item.name
        row("C").value = item.idCard
      }

      val path = outputDir /
        s"${year}年${month.stripPrefix("0")}月待遇支付失败人员名单${Formatter.formatDate()}.xls"

      println(s"\n保存: $path")

      workbook.save(path)

      println("结束导出数据")
    }
  }
}

class UncertPauseList extends Subcommand("pauseList") {
  descr("从业务系统导出未认证已停保人员名单")

  val outputDir = """D:\待遇核定"""

  override def execute(): Unit = {
    println("开始导出数据")

    val exportFile = Files.createTempFile("yhsb", ".xls").toString
    Session.use() {
      _.exportAllTo(
        RetiredPersonPauseQuery(),
        RetiredPersonPauseQuery.columnMap
      )(
        exportFile
      )
    }

    val workbook = Excel.load(exportFile)
    val sheet = workbook.getSheetAt(0)
    sheet.setColumnWidth(0, 35 * 256)
    sheet.setColumnWidth(2, 20 * 256)
    sheet.setColumnWidth(3, 8 * 256)

    sheet.deleteRowIf(startRow = 1) {
      _("H").value == "月度拨付触发暂停"
    }

    workbook.saveAfter(
      outputDir / s"截至目前未认证已停发待遇人员名单${Formatter.formatDate()}.xls"
    ) { path =>
      println(s"保存: $path")
    }

    println("结束导出数据")
  }
}

class UncertList extends Subcommand("uncertList") {
  descr("从业务系统下载未认证人员名单")

  val startDate = trailArg[String](
    descr = "认证开始年月, 格式: YYYYMM, 例如: 202101"
  )

  val endDate = trailArg[String](
    descr = "认证结束年月, 格式: YYYYMM, 例如: 202101"
  )

  val outputDir = """D:\待遇核定"""

  override def execute(): Unit = {
    println("开始导出数据")

    val exportFile = Files.createTempFile("yhsb", ".xls").toString
    Session.use() {
      _.exportAllTo(
        CertedPersonQuery(
          startNextCentDate = startDate(),
          endNextCentDate = endDate()
        ),
        CertedPersonQuery.columnMap
      )(
        exportFile
      )
    }

    val workbook = Excel.load(exportFile)
    val sheet = workbook.getSheetAt(0)
    sheet.setColumnWidth(0, 35 * 256)
    sheet.setColumnWidth(2, 8 * 256)
    sheet.setColumnWidth(3, 20 * 256)

    workbook.saveAfter {
      val (sy, sm, _) = Formatter.splitDate(startDate())
      val (ey, em, _) = Formatter.splitDate(endDate())
      val dateRange = if (sy == ey) {
        if (sm == em) {
          s"${sy}年${sm.stripPrefix("0")}月"
        } else {
          s"${sy}年${sm.stripPrefix("0")}-${em.stripPrefix("0")}月"
        }
      } else {
        s"${sy}年${sm.stripPrefix("0")}月-${ey}年${em.stripPrefix("0")}月"
      }
      outputDir / s"${dateRange}认证即将到期的待遇人员名单${Formatter.formatDate()}.xls"
    } { path =>
      println(s"保存: $path")
    }

    println("结束导出数据")
  }
}

class ArrearList extends Subcommand("arrearList") {
  descr("从业务系统下载欠费人员名单")

  val retireDate = trailArg[String](
    descr = "预算到龄日期, 格式: YYYYMMDD, 例如: 20210131"
  )

  val outputDir = """D:\待遇核定"""

  override def execute(): Unit = {
    println("开始导出数据")

    val exportFile = Files.createTempFile("yhsb", ".xls").toString
    Session.use() {
      _.exportAllTo(
        RetiringPersonQuery(
          retireDate = Formatter.toDashedDate(retireDate()),
          inArrear = "1",
          cbState = "1",
        ),
        RetiringPersonQuery.columnMap
      )(
        exportFile
      )
    }

    val workbook = Excel.load(exportFile)
    val sheet = workbook.getSheetAt(0)
    sheet.setColumnWidth(0, 35 * 256)
    sheet.setColumnWidth(2, 8 * 256)
    sheet.setColumnWidth(3, 20 * 256)
    sheet.setColumnWidth(11, 30 * 256)

    workbook.saveAfter(
      outputDir / s"截至目前到龄欠费人员名单${Formatter.formatDate()}.xls"
    ) { path =>
      println(s"保存: $path")
    }

    println("结束导出数据")
  }
}

