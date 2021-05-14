import java.io.OutputStream
import java.nio.file.Files

import yhsb.base.command._
import yhsb.base.datetime.Formatter
import yhsb.base.datetime.YearMonth
import yhsb.base.excel.Excel._
import yhsb.base.io.Path._
import yhsb.base.math.Number.OptionBigDecimalOps
import yhsb.base.run.process
import yhsb.base.text.String._
import yhsb.base.util.Config
import yhsb.base.util.OptionalOps
import yhsb.base.util.UtilOps
import yhsb.cjb.net.Session
import yhsb.cjb.net.protocol.CBState
import yhsb.cjb.net.protocol.PayStopReason
import yhsb.cjb.net.protocol.PayingInfoInProvinceQuery
import yhsb.cjb.net.protocol.PayingInfoInProvinceQuery.PayInfoRecord
import yhsb.cjb.net.protocol.PayingInfoInProvinceQuery.PayInfoTotalRecord
import yhsb.cjb.net.protocol.PayingPersonStopAuditDetailQuery
import yhsb.cjb.net.protocol.PayingPersonStopAuditQuery
import yhsb.cjb.net.protocol.PaymentTerminateQuery
import yhsb.cjb.net.protocol.PersonInfoInProvinceQuery
import yhsb.cjb.net.protocol.PersonInfoPaylistQuery
import yhsb.cjb.net.protocol.PersonInfoQuery
import yhsb.cjb.net.protocol.RefundQuery
import yhsb.cjb.net.protocol.Result
import yhsb.cjb.net.protocol.RetiredPersonStopAuditDetailQuery
import yhsb.cjb.net.protocol.RetiredPersonStopAuditQuery
import yhsb.cjb.net.protocol.TreatmentReviewQuery
import yhsb.cjb.net.protocol.JoinTerminateQuery
import yhsb.cjb.net.protocol.JoinStopReason
import yhsb.qb.net.{Session => QBSession}
import yhsb.qb.net.protocol.RetiredPersonStopQuery

class Query(args: collection.Seq[String]) extends Command(args) {

  banner("数据查询处理程序")

  val doc =
    new Subcommand("doc") with InputFile {
      descr("档案目录生成")

      def execute() = {
        val workbook = Excel.load(inputFile())
        val sheet = workbook.getSheetAt(0)

        Session.use() { session =>
          for (i <- 0 to sheet.getLastRowNum) {
            val row = sheet.getRow(i)
            val idCard = row.getCell("A").value
            val title = row.getCell("D").value

            val result = session.request(PersonInfoInProvinceQuery(idCard))
            if (result.isEmpty || result(0).idCard == null) {
              System.err.println(s"Error: ${i + 1} $idCard")
              System.exit(-1)
            } else {
              val info = result(0)
              println(s"${i + 1} ${info.name}")

              row
                .getOrCreateCell("E")
                .setCellValue(
                  s"${info.name}$title"
                )
            }
          }
        }
        workbook.save(inputFile().insertBeforeLast(".upd"))
      }
    }

  val up =
    new Subcommand("up") with InputFile with RowRange {
      descr("更新参保信息")

      val nameRow = trailArg[String](descr = "姓名列名称")
      val idCardRow = trailArg[String](descr = "身份证列名称")
      val updateRow = trailArg[String](descr = "更新列名称")
      val neighborhoodRow =
        opt[String](name = "xzj", short = 'x', descr = "更新乡镇街列名称")
      val nameComparedRow =
        opt[String](name = "mzbd", short = 'm', descr = "更新姓名比对列名称")

      def execute(): Unit = {
        val workbook = Excel.load(inputFile())
        val sheet = workbook.getSheetAt(0)

        Session.use() { session =>
          for (i <- (startRow() - 1) until endRow()) {
            val row = sheet.getRow(i)
            val name = row.getCell(nameRow()).value.trim()
            val idCard = row.getCell(idCardRow()).value.trim().toUpperCase()

            println(idCard)

            val result = session.request(PersonInfoInProvinceQuery(idCard))
            result.map(item => {
              row
                .getOrCreateCell(updateRow())
                .setCellValue(item.jbState)
              if (neighborhoodRow.isDefined) {
                row
                  .getOrCreateCell(neighborhoodRow())
                  .setCellValue(item.dwName.get)
              }
              if (nameComparedRow.isDefined && item.name != name) {
                row
                  .getOrCreateCell(nameComparedRow())
                  .setCellValue(item.name)
              }
            })
          }
        }
        workbook.save(inputFile().insertBeforeLast(".upd"))
      }
    }

  val payInfo =
    new Subcommand("pay") with Export {
      descr("缴费信息查询")

      val print = opt[Boolean](required = false, descr = "是否直接打印")

      val idCard = trailArg[String](descr = "身份证号码")

      def printInfo(info: PersonInfoInProvinceQuery.Item) = {
        println("个人信息:")
        println(
          s"${info.name} ${info.idCard} ${info.jbState} " +
            s"${info.jbKind} ${info.agency} ${info.czName} " +
            s"${info.opTime}\n"
        )
      }

      def printPayInfoRecords(
          records: collection.Seq[PayInfoRecord],
          message: String
      ) = {
        println(message)
        println(
          s"${"序号".padLeft(4)}${"年度".padLeft(5)}" +
            s"${"个人缴费".padLeft(10)}${"省级补贴".padLeft(9)}" +
            s"${"市级补贴".padLeft(9)}${"县级补贴".padLeft(9)}" +
            s"${"政府代缴".padLeft(9)}${"集体补助".padLeft(9)}" +
            s"${"退渔补助".padLeft(9)}  社保经办机构 划拨时间"
        )

        def format(r: PayInfoRecord) = {
          r match {
            case t: PayInfoTotalRecord =>
              s"合计${r.personal.padLeft(9)}${r.provincial.padLeft(9)}" +
                s"${r.civic.padLeft(9)}${r.prefectural.padLeft(9)}" +
                s"${r.governmentalPay.padLeft(9)}${r.communalPay.padLeft(9)}" +
                s"${r.fishmanPay.padLeft(9)}   " +
                s"总计: ${t.total.getOrElse(0)}".padLeft(9)
            case _ =>
              s"${r.year.toString.padLeft(4)}${r.personal.padLeft(9)}" +
                s"${r.provincial.padLeft(9)}${r.civic.padLeft(9)}" +
                s"${r.prefectural.padLeft(9)}${r.governmentalPay.padLeft(9)}" +
                s"${r.communalPay.padLeft(9)}${r.fishmanPay.padLeft(9)}   " +
                s"${r.agencies.mkString("|")} ${r.transferDates.mkString("|")}"
          }
        }

        var i = 1
        for (r <- records) {
          r match {
            case _: PayInfoTotalRecord =>
              println(s"     ${format(r)}")
            case _ =>
              println(s"${i.toString.padLeft(3)}  ${format(r)}")
              i += 1
          }
        }
      }

      override def execute(): Unit = {
        val (info, payInfoResult) = Session.use() { session =>
          val info =
            session
              .request(PersonInfoInProvinceQuery(idCard()))
              .let { result =>
                if (result.isEmpty || result(0).invalid) null else result(0)
              }

          val payInfoResult =
            session
              .request(PayingInfoInProvinceQuery(idCard()))
              .let { result =>
                if (result.isEmpty || result.size == 1 && result(0).year == 0)
                  null
                else result
              }

          (info, payInfoResult)
        }

        if (info == null) {
          println("未查到参保记录")
          return
        }

        printInfo(info)

        val (records, _) = if (payInfoResult == null) {
          println("未查询到缴费信息")
          (null, null)
        } else {
          val (payedRecords, unpayedRecords) = payInfoResult.getPayInfoRecords()

          printPayInfoRecords(payedRecords, "已拨付缴费历史记录:")
          if (unpayedRecords.nonEmpty) {
            printPayInfoRecords(unpayedRecords, "\n未拨付补录入记录:")
          }

          (payedRecords, unpayedRecords)
        }

        if ((export() || print()) && records != null && records.nonEmpty) {
          val path = """D:\征缴管理"""
          val xlsx = """雨湖区城乡居民基本养老保险缴费查询单模板.xlsx"""

          val workbook = Excel.load(path / xlsx)
          val sheet = workbook.getSheetAt(0)
          sheet("A5").value = info.name
          sheet("C5").value = info.idCard
          sheet("E5").value = info.agency
          sheet("G5").value = info.czName
          sheet("L5").value = info.opTime

          var startRow, currentRow = 8
          for (r <- records) {
            val row = sheet.getOrCopyRow(currentRow, startRow)
            currentRow += 1

            row("A").let { cell =>
              r match {
                case _: PayInfoTotalRecord => cell.setBlank()
                case _                     => cell.value = currentRow - startRow
              }
            }

            row("B").let { cell =>
              r match {
                case _: PayInfoTotalRecord => cell.value = "合计"
                case _                     => cell.value = r.year
              }
            }

            row("C").value = r.personal.getOrElse(BigDecimal(0))
            row("D").value = r.provincial.getOrElse(BigDecimal(0))
            row("E").value = r.civic.getOrElse(BigDecimal(0))
            row("F").value = r.prefectural.getOrElse(BigDecimal(0))
            row("G").value = r.governmentalPay.getOrElse(BigDecimal(0))
            row("H").value = r.communalPay.getOrElse(BigDecimal(0))
            row("I").value = r.fishmanPay.getOrElse(BigDecimal(0))
            row("J").value = r match {
              case _: PayInfoTotalRecord => "总计"
              case _                     => r.agencies.mkString("|")
            }
            row("L").let { cell =>
              r match {
                case t: PayInfoTotalRecord => cell.value = t.total
                case _                     => cell.value = r.transferDates.mkString("|")
              }
            }
          }

          if (export()) {
            val file = path / s"${info.name}缴费查询单.xlsx"
            println(s"\n保存: $file")
            workbook.save(file)
          }
          if (print()) {
            val file = Files.createTempFile("yhsb", ".xlsx")
            workbook.save(file)
            val cmd =
              s"""${Config.load("cmd").getString("print.excel")} "$file""""
            println(s"\n打印: $cmd")
            process.execute(
              cmd,
              OutputStream.nullOutputStream(),
              OutputStream.nullOutputStream()
            )
          }
        }
      }
    }

  val paySpan =
    new Subcommand("paySpan") with InputFile with RowRange {
      descr("查询时间段内基础养老金情况")

      val queryQBData = opt[Boolean](required = false, descr = "是否查询企保终止数据")

      def execute(): Unit = {
        val workbook = Excel.load(inputFile())
        val sheet = workbook.getSheetAt(0)

        try {
          if (queryQBData()) {
            QBSession.use("sqb") { session =>
              for {
                i <- (startRow() - 1) until endRow()
                row = sheet.getRow(i)
                name = row("C").value.trim()
                idCard = row("B").value.trim().toUpperCase()
              } {
                print(s"$i $idCard $name 查询企保终止情况: ")
                session
                  .request(RetiredPersonStopQuery(idCard))
                  .resultSet
                  .headOption match {
                  case None => println("未查询到记录")
                  case Some(it) =>
                    row.getOrCreateCell("J").value = it.retiredDate
                    row.getOrCreateCell("K").value = it.payStartYearMonth
                    row.getOrCreateCell("L").value = it.stopYearMonth
                    println(
                      s"${it.retiredDate} ${it.payStartYearMonth} ${it.stopYearMonth}"
                    )
                }
              }
            }
          }

          Session.use() { session =>
            for {
              i <- (startRow() - 1) until endRow()
              row = sheet.getRow(i)
              name = row("C").value.trim()
              idCard = row("B").value.trim().toUpperCase()
              if row("K").value != "" && row("L").value != ""
              startYearMonth = row("K").value.toInt
              endYearMonth = row("L").value.toInt
              //if row("M").value == ""
              //if row("P").value != ""
            } {
              print(s"$i $idCard $name ${startYearMonth}-${endYearMonth} ")

              val pInfo = session.request(PersonInfoQuery(idCard)).headOption

              def clearCells() = {
                row.getOrCreateCell("M").setBlank()
                row.getOrCreateCell("N").setBlank()
                row.getOrCreateCell("O").setBlank()
                row.getOrCreateCell("P").setBlank()
                row.getOrCreateCell("Q").setBlank()
                row.getOrCreateCell("R").setBlank()
              }

              def setMemo(memo: String) = {
                println(memo)
                row.getOrCreateCell("R").value = memo
              }

              clearCells()

              if (pInfo.isEmpty) {
                setMemo("未在我区参保")
              } else {
                val startTime = session
                  .request(TreatmentReviewQuery(idCard, reviewState = "1"))
                  .lastOption
                  .map(_.payMonth) match {
                  case None       => startYearMonth // 无法取得待遇开始时间
                  case Some(time) => time
                }

                sealed abstract class StopTime {
                  def str: String
                }
                case class RetiredStopTime(time: Int) extends StopTime {
                  override def str: String = time.toString()
                }
                case class PayingStopTime(time: Int) extends StopTime {
                  override def str: String = time.toString()
                }
                case class NonStopTime() extends StopTime {
                  override def str: String = ""
                }

                val (stopTime: StopTime, endTime) = session
                  .request(RetiredPersonStopAuditQuery(idCard, "1"))
                  .lastOption match {
                  case Some(item) =>
                    val detail = session
                      .request(RetiredPersonStopAuditDetailQuery(item))
                      .head
                    val refundAmount = detail.refundAmount
                    val deductAmount = detail.deductAmount
                    if (
                      (refundAmount != "" && refundAmount != "0") ||
                      (deductAmount != "" && deductAmount != "0")
                    ) { // 有稽核金额
                      (RetiredStopTime(item.stopYearMonth), endYearMonth)
                    } else {
                      (RetiredStopTime(item.stopYearMonth), item.stopYearMonth)
                    }
                  case None =>
                    session
                      .request(PayingPersonStopAuditQuery(idCard, "1"))
                      .lastOption match {
                      case Some(item) =>
                        (PayingStopTime(item.stopYearMonth), endYearMonth)
                      case None =>
                        (NonStopTime, endYearMonth)
                    }
                }

                if (stopTime.isInstanceOf[PayingStopTime]) {
                  setMemo("缴费终止人员")
                  row.getOrCreateCell("Q").value = stopTime.str
                } else if (endTime < startYearMonth) {
                  setMemo("企保领待前已终止")
                  row.getOrCreateCell("Q").value = stopTime.str
                } else {
                  val dupStartTime = startTime.max(startYearMonth)
                  val dupEndTime = endTime.min(endYearMonth)

                  val dupSpan = s"$dupStartTime-$dupEndTime"
                  val afterSpan =
                    if (endTime > endYearMonth) {
                      s"${YearMonth.from(endYearMonth).offset(1).toYearMonth}-$endTime"
                    } else {
                      ""
                    }

                  val dupAmount = session
                    .request(PersonInfoPaylistQuery(pInfo.get))
                    .filter { it =>
                      it.payYearMonth >= dupStartTime &&
                      it.payYearMonth <= dupEndTime &&
                      //it.payItem.startsWith("基础养老金") &&
                      it.payState == "已支付"
                    }
                    .map(_.amount)
                    .sum

                  val refund = session
                    .request(RefundQuery(idCard))
                    .filter { it =>
                      it.state == "已到账"
                    }
                    .map(_.amount)
                    .sum

                  println(s"重复: $dupSpan  $dupAmount $refund $afterSpan")
                  row.getOrCreateCell("M").value = dupSpan
                  row.getOrCreateCell("N").value = dupAmount
                  row.getOrCreateCell("O").value = refund
                  row.getOrCreateCell("P").value = afterSpan
                  row.getOrCreateCell("Q").value = stopTime.str
                }
              }
            }
          }
        } finally {
          workbook.save(inputFile().insertBeforeLast(".upd"))
        }
      }
    }

  val refund =
    new Subcommand("refund") with InputFile with RowRange {
      descr("查询已稽核金额")

      def execute(): Unit = {
        val workbook = Excel.load(inputFile())
        val sheet = workbook.getSheetAt(0)

        try {
          Session.use() { session =>
            for (i <- (startRow() - 1) until endRow()) {
              val row = sheet.getRow(i)
              val name = row("C").value.trim()
              val idCard = row("B").value.trim().toUpperCase()
              val sAmount = row("N").value.trim()
              if (sAmount != "") {
                val amount = sAmount.toInt
                val refund = session
                  .request(RefundQuery(idCard))
                  .filter { it =>
                    it.state == "已到账"
                  }
                  .map(_.amount)
                  .sum

                println(s"$i $idCard $name $amount $refund")

                if (refund > 0) {
                  row.getOrCreateCell("O").value = refund
                }
              }
            }
          }
        } finally {
          workbook.save(inputFile().insertBeforeLast(".upd"))
        }
      }
    }

  val audit =
    new Subcommand("audit") with InputFile with RowRange {
      descr("测试应稽核金额")

      def execute(): Unit = {
        val workbook = Excel.load(inputFile())
        val sheet = workbook.getSheetAt(0)

        try {
          Session.use() { session =>
            for (i <- (startRow() - 1) until endRow()) {
              val row = sheet.getRow(i)
              val name = row("F").value.trim()
              val idCard = row("E").value.trim().toUpperCase()
              val time = {
                val t = row("I").value.trim()
                if ("""\d\d\d\d\d\d.*""".r.matches(t)) {
                  val yearMonth = t.substring(0, 6)
                  if (yearMonth >= "190101" && yearMonth <= "209912") {
                    Some(s"${yearMonth.insertBeforeIndex(4, "-")}")
                  } else {
                    None
                  }
                } else {
                  None
                }
              }

              var memo = ""
              var amount = ""
              if (time.isEmpty) {
                memo = "日期无效"
              } else {
                session.request(PersonInfoQuery(idCard)).headOption match {
                  case None => memo = "未在我区参保"
                  case Some(it) =>
                    if (it.cbState == CBState.Stopped) {
                      memo = "终止人员"
                    } else if (
                      it.cbState == CBState.Normal ||
                      it.cbState == CBState.Paused
                    ) {
                      session
                        .request(
                          PaymentTerminateQuery(
                            it,
                            time.get,
                            PayStopReason.Death
                          )
                        )
                        .headOption match {
                        case None =>
                          session
                            .request(
                              JoinTerminateQuery(
                                it,
                                time.get,
                                JoinStopReason.Death
                              )
                            )
                            .headOption match {
                            case Some(it) =>
                              if (it.payAmount != "" && it.payAmount != "0") {
                                memo = "有个人账户可清退"
                              }
                            case None =>
                          }
                        case Some(it) => {
                          amount = it.auditAmount
                          if (it.payAmount != "" && it.payAmount != "0") {
                            memo = "有个人账户可清退"
                          }
                        }
                      }
                    }
                }
              }
              println(s"$i $idCard $name ${amount.padLeft(8, ' ')} $memo")
              row.getOrCreateCell("J").value = amount
              row.getOrCreateCell("K").value = memo
            }
          }
        } finally {
          workbook.save(inputFile().insertBeforeLast(".upd"))
        }
      }
    }

  val division =
    new Subcommand("division") with InputFile with RowRange {
      descr("查询乡镇街道、村社区")

      def execute(): Unit = {
        val workbook = Excel.load(inputFile())
        val sheet = workbook.getSheetAt(0)

        try {
          Session.use() { session =>
            for {
              i <- (startRow() - 1) until endRow()
              row = sheet.getRow(i)
              name = row("C").value.trim()
              idCard_ = row("F").value.trim()
            } {
              print(s"$i $idCard_ $name ")

              val len = idCard_.length()
              if (len >= 18) {
                val idCard =
                  if (len > 18) idCard_.substring(0, 18).toUpperCase()
                  else idCard_.toUpperCase()

                session
                  .request(PersonInfoQuery(idCard))
                  .headOption match {
                  case None =>
                    println("未在我区参保")
                  case Some(it) =>
                    println(s"${it.dwName.getOrElse("")} ${it.csName.getOrElse("")}")
                    row.getOrCreateCell("K").value = it.dwName
                    row.getOrCreateCell("L").value = it.csName
                }
              } else {
                println("身份证号码有误")
              }
            }
          }
        } finally {
          workbook.save(inputFile().insertBeforeLast(".upd"))
        }
      }
    }

  addSubCommand(doc)
  addSubCommand(up)
  addSubCommand(payInfo)
  addSubCommand(paySpan)
  addSubCommand(refund)
  addSubCommand(audit)
  addSubCommand(division)
}

object Main {
  def main(args: Array[String]) = new Query(args).runCommand()
}
