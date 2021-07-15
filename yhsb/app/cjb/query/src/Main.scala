import java.io.OutputStream
import java.nio.file.Files

import scala.collection.SeqMap
import scala.collection.SortedMap
import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.TreeMap

import yhsb.base.command._
import yhsb.base.datetime.Formatter
import yhsb.base.datetime.YearMonth
import yhsb.base.datetime.YearMonthRange
import yhsb.base.excel.Excel._
import yhsb.base.io.Path._
import yhsb.base.math.Number.OptionBigDecimalOps
import yhsb.base.run.process
import yhsb.base.text.String._
import yhsb.base.util.Config
import yhsb.base.util.OptionalOps
import yhsb.base.util.UtilOps
import yhsb.cjb.net.Session
import yhsb.cjb.net.protocol.BankInfoQuery
import yhsb.cjb.net.protocol.CBState
import yhsb.cjb.net.protocol.JoinStopReason
import yhsb.cjb.net.protocol.JoinTerminateQuery
import yhsb.cjb.net.protocol.PayStopReason
import yhsb.cjb.net.protocol.PayingInfoInProvinceQuery
import yhsb.cjb.net.protocol.PayingInfoInProvinceQuery.PayInfoRecord
import yhsb.cjb.net.protocol.PayingInfoInProvinceQuery.PayInfoTotalRecord
import yhsb.cjb.net.protocol.PaymentQuery
import yhsb.cjb.net.protocol.PaymentTerminateQuery
import yhsb.cjb.net.protocol.PersonInfoInProvinceQuery
import yhsb.cjb.net.protocol.PersonInfoPaylistQuery
import yhsb.cjb.net.protocol.PersonInfoQuery
import yhsb.cjb.net.protocol.PersonInfoTreatmentAdjustDetailQuery
import yhsb.cjb.net.protocol.PersonInfoTreatmentAdjustQuery
import yhsb.cjb.net.protocol.RefundQuery
import yhsb.cjb.net.protocol.Result
import yhsb.cjb.net.protocol.RetiredPersonStopAuditDetailQuery
import yhsb.cjb.net.protocol.SessionOps.PersonStopAuditQuery
import yhsb.cjb.net.protocol.TreatmentReviewQuery
import yhsb.cjb.net.protocol.WorkingPersonStopAuditDetailQuery
import yhsb.qb.net.protocol.RetiredPersonStopQuery
import yhsb.qb.net.{Session => QBSession}
import yhsb.cjb.net.protocol.RetiringPersonQuery
import yhsb.cjb.net.protocol.PayListQuery
import yhsb.cjb.net.protocol.PayState
import yhsb.cjb.net.protocol.PayListPersonalDetailQuery
import yhsb.cjb.net.protocol.CertedPersonQuery
import scala.collection.mutable.ArrayBuffer
import yhsb.cjb.net.protocol.CertFlag
import yhsb.cjb.net.protocol.CertType
import yhsb.cjb.net.protocol.RetiredPersonPauseAuditQuery
import yhsb.cjb.net.protocol.SuspectedDeathQuery
import yhsb.cjb.net.protocol.DataCompareQuery

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

            if (idCard != "") {
              val result = session.request(PersonInfoInProvinceQuery(idCard))
              result.map(item => {
                row
                  .getOrCreateCell(updateRow())
                  .setCellValue(item.jbState)
                if (neighborhoodRow.isDefined) {
                  row
                    .getOrCreateCell(neighborhoodRow())
                    .setCellValue(item.dwName.getOrElse(""))
                }
                if (nameComparedRow.isDefined && item.name != name) {
                  row
                    .getOrCreateCell(nameComparedRow())
                    .setCellValue(item.name)
                }
              })
            }
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
              if startYearMonth > 190001
              if endYearMonth > 190001
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
                  .retiredPersonStopAuditQuery(idCard, "1")
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
                      .payingPersonStopAuditQuery(
                        idCard,
                        "1"
                      )
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
                val t = row("M").value.trim()
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
              row.getOrCreateCell("N").value = amount
              row.getOrCreateCell("P").value = memo
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
              idCard_ = row("B").value.trim()
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
                    println(
                      s"${it.dwName.getOrElse("")} ${it.csName.getOrElse("")}"
                    )
                    if (name == "") {
                      row.getOrCreateCell("C").value = it.name
                    }
                    row.getOrCreateCell("D").value = it.dwName
                    row.getOrCreateCell("E").value = it.csName
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

  val delayPay =
    new Subcommand("delayPay") with InputFile with RowRange {
      descr("查询补发基础养老金情况")

      val startMonth = trailArg[Int](descr = "开始发放月份, 如: 202001")
      val endMonth = trailArg[Int](descr = "结束发放月份, 如: 202012")

      def mergeYearMonthMap(
          map: SortedMap[YearMonth, BigDecimal]
      ): Option[SeqMap[YearMonthRange, BigDecimal]] = {
        if (map.isEmpty) {
          None
        } else {
          var startYearMonth: Option[YearMonth] = None
          var endYearMonth: Option[YearMonth] = None
          var amount: Option[BigDecimal] = None
          var resultMap = LinkedHashMap[YearMonthRange, BigDecimal]()
          for ((yearMonth, value) <- map) {
            if (startYearMonth.isEmpty) {
              startYearMonth = Some(yearMonth)
              endYearMonth = Some(yearMonth)
              amount = Some(value)
            } else if (
              yearMonth.offset(-1) == endYearMonth.get &&
              value == amount.get
            ) {
              endYearMonth = Some(yearMonth)
            } else {
              resultMap(YearMonthRange(startYearMonth.get, endYearMonth.get)) =
                amount.get
              startYearMonth = Some(yearMonth)
              endYearMonth = Some(yearMonth)
              amount = Some(value)
            }
          }
          resultMap(YearMonthRange(startYearMonth.get, endYearMonth.get)) =
            amount.get
          Some(resultMap)
        }
      }

      def normalize(dec: BigDecimal): String = {
        if (dec.isValidInt) s"${dec.toBigInt}"
        else f"$dec%.2f"
      }

      def normalize(
          map: SeqMap[YearMonthRange, BigDecimal]
      ): Option[String] = {
        if (map.isEmpty) {
          None
        } else {
          Some(
            map
              .map { case (k, v) =>
                if (k.start == k.end) s"${k.start}:${normalize(v)}"
                else s"$k:${normalize(v)}"
              }
              .mkString("|")
          )
        }
      }

      def normalize(range: YearMonthRange): Option[String] = {
        Some(
          if (range.start == range.end) s"${range.start}"
          else s"${range.start}-${range.end}"
        )
      }

      def option(i: Int) = if (i == 0) None else Some(i)

      def execute(): Unit = {
        val workbook = Excel.load(inputFile())
        val sheet = workbook.getSheetAt(0)
        try {
          Session.use() { session =>
            for {
              i <- (startRow() - 1) until endRow()
              row = sheet.getRow(i)
              name = row("F").value.trim()
              idCard = row("E").value.trim()
            } {
              val pInfo = session.request(PersonInfoQuery(idCard)).headOption

              val cbState =
                pInfo match {
                  case None        => CBState.NoJoined
                  case Some(value) => value.cbState
                }

              val startTreatmentTime = session
                .request(TreatmentReviewQuery(idCard, reviewState = "1"))
                .lastOption
                .map(_.payMonth)

              val endTreatmentTime = if (cbState == CBState.Stopped) {
                session
                  .retiredPersonStopAuditQuery(idCard, "1")
                  .lastOption
                  .map(_.stopYearMonth)
              } else {
                None
              }

              var (payAmount, payDetail, payCount, accountYearMonth) =
                if (pInfo.isDefined) {
                  val payMap = TreeMap[YearMonth, BigDecimal]()
                  var accountYearMonth: Option[Int] = None
                  var payAmount: Option[BigDecimal] = None
                  for (
                    item <- session.request(PersonInfoPaylistQuery(pInfo.get))
                    if item.accountYearMonth >= startMonth() &&
                      item.accountYearMonth <= endMonth() &&
                      item.payYearMonth < startMonth() &&
                      item.payItem.startsWith("基础养老金") &&
                      item.payState == "已支付"
                  ) {
                    accountYearMonth = Some(item.accountYearMonth)
                    payAmount += item.amount

                    val payYearMonth = YearMonth.from(item.payYearMonth)

                    if (!payMap.contains(payYearMonth)) {
                      payMap(payYearMonth) = item.amount
                    } else {
                      payMap(payYearMonth) += item.amount
                    }
                  }
                  (
                    payAmount,
                    mergeYearMonthMap(payMap),
                    option(payMap.keySet.size),
                    accountYearMonth
                  )
                } else {
                  (None, None, None, None)
                }

              var payStandard = if (pInfo.isDefined) {
                val payMap = TreeMap[YearMonth, BigDecimal]()
                for (
                  item <- session.request(PersonInfoPaylistQuery(pInfo.get))
                  if item.payYearMonth >= startMonth() &&
                    item.payYearMonth <= endMonth() &&
                    item.payItem.startsWith("基础养老金") &&
                    item.payState == "已支付"
                ) {
                  val payYearMonth = YearMonth.from(item.payYearMonth)

                  if (!payMap.contains(payYearMonth)) {
                    payMap(payYearMonth) = item.amount
                  } else {
                    payMap(payYearMonth) += item.amount
                  }
                }
                mergeYearMonthMap(payMap)
              } else {
                None
              }

              val reg = """(\d\d\d\d)-(\d\d)-(\d\d)""".r
              val bankCardTime = session
                .request(BankInfoQuery(idCard))
                .headOption
                .flatMap(_.registerTime match {
                  case reg(y, m, d) =>
                    Some(y.toInt * 10000 + m.toInt * 100 + d.toInt)
                  case _ => None
                })

              var memo: Option[String] = None

              var adjustTimeByCert: Option[YearMonthRange] = None
              var adjustTimeByAudit: Option[YearMonthRange] = None
              var adjustTimeByStop: Option[YearMonthRange] = None

              if (pInfo.isDefined) {
                var done = false

                for {
                  it <- session.request(
                    PersonInfoTreatmentAdjustQuery(pInfo.get)
                  )
                  if !done
                  if it.endYearMonth >= startMonth() && it.endYearMonth <= endMonth()
                } {
                  if (!done && it.memo.contains("生存认证触发")) {
                    adjustTimeByCert = Some(
                      YearMonthRange(
                        YearMonth.from(it.startYearMonth),
                        YearMonth.from(it.endYearMonth)
                      )
                    )
                    if (
                      payDetail.isDefined &&
                      payDetail.get.head._1.start.toYearMonth == it.startYearMonth &&
                      accountYearMonth.get == it.endYearMonth
                    ) {
                      memo = Some("生存认证后系统自动补发之前未认证而停发的养老金")
                      done = true
                    } else if (
                      payDetail.isDefined &&
                      accountYearMonth.get == it.endYearMonth
                    ) {
                      session
                        .request(PersonInfoTreatmentAdjustDetailQuery(it))
                        .filter(_.adjustType.startsWith("001"))
                        .map(_.payYearMonth)
                        .minOption match {
                        case Some(ym) =>
                          if (payDetail.get.head._1.start.toYearMonth == ym) {
                            adjustTimeByCert = Some(
                              YearMonthRange(
                                YearMonth.from(ym),
                                YearMonth.from(it.endYearMonth)
                              )
                            )
                            memo = Some("生存认证后系统自动补发之前未认证而停发的养老金")
                            done = true
                          }
                        case _ =>
                      }
                    }
                  }

                  if (!done && it.memo.contains("核定补发")) {
                    adjustTimeByAudit = Some(
                      YearMonthRange(
                        YearMonth.from(it.startYearMonth),
                        YearMonth.from(it.endYearMonth)
                      )
                    )
                    if (
                      payDetail.isDefined &&
                      payDetail.get.head._1.start.toYearMonth == it.startYearMonth &&
                      accountYearMonth.get == it.endYearMonth
                    ) {
                      memo = Some("待遇核定后系统补发从待遇领取月份开始时的养老金")
                      done = true
                    }
                  }

                  if (!done && it.memo.contains("待遇终止补发")) {
                    adjustTimeByStop = Some(
                      YearMonthRange(
                        YearMonth.from(it.startYearMonth),
                        YearMonth.from(it.endYearMonth)
                      )
                    )
                    if (payDetail.isEmpty) {
                      memo = Some("待遇终止后系统补发到终止月份为止未发放的养老金")
                      done = true

                      payAmount = None
                      payDetail = None
                      payCount = None
                      accountYearMonth = None

                      val payMap = TreeMap[YearMonth, BigDecimal]()
                      val stdMap = TreeMap[YearMonth, BigDecimal]()
                      for (
                        item <- session.request(
                          PersonInfoTreatmentAdjustDetailQuery(it)
                        )
                        if item.adjustType.startsWith("001")
                      ) {
                        if (
                          item.payYearMonth < startMonth() &&
                          item.accountYearMonth >= startMonth() &&
                          item.accountYearMonth <= endMonth()
                        ) {
                          accountYearMonth = Some(item.accountYearMonth)
                          payAmount += item.amount

                          val payYearMonth = YearMonth.from(item.payYearMonth)

                          if (!payMap.contains(payYearMonth)) {
                            payMap(payYearMonth) = item.amount
                          } else {
                            payMap(payYearMonth) += item.amount
                          }
                        } else if (
                          item.payYearMonth >= startMonth() &&
                          item.payYearMonth <= endMonth()
                        ) {
                          val payYearMonth = YearMonth.from(item.payYearMonth)

                          if (!stdMap.contains(payYearMonth)) {
                            stdMap(payYearMonth) = item.amount
                          } else {
                            stdMap(payYearMonth) += item.amount
                          }
                        }
                      }
                      payDetail = mergeYearMonthMap(payMap)
                      payStandard = mergeYearMonthMap(stdMap)
                      payCount = option(payMap.keySet.size)
                      if (payDetail.isDefined && payStandard.isDefined) {
                        adjustTimeByStop = Some(
                          YearMonthRange(
                            payDetail.get.head._1.start,
                            payStandard.get.last._1.end
                          )
                        )
                      }
                    }
                  }
                }

                if (
                  !done &&
                  bankCardTime.isDefined && accountYearMonth.isDefined
                ) {
                  val bankYearMonth = YearMonth.from(bankCardTime.get / 100)
                  val accYearMonth = YearMonth.from(accountYearMonth.get)

                  if (
                    bankYearMonth == accYearMonth || bankYearMonth.offset(
                      1
                    ) == accYearMonth
                  ) {
                    memo = Some("绑定银行卡后系统自动补发未绑卡时支付失败月份的养老金")
                    done = true
                  }
                }
              }

              val strPayDetail = payDetail.flatMap(normalize)
              val strPayStandard = payStandard.flatMap(normalize)
              val strAdjustTimeByCert = adjustTimeByCert.flatMap(normalize)
              val strAdjustTimeByAudit = adjustTimeByAudit.flatMap(normalize)
              val strAdjustTimeByStop = adjustTimeByStop.flatMap(normalize)

              row.getOrCreateCell("Q").value = cbState.toString()
              row.getOrCreateCell("R").value = startTreatmentTime
              row.getOrCreateCell("S").value = endTreatmentTime
              row.getOrCreateCell("T").value = payAmount
              row.getOrCreateCell("U").value = payCount
              row.getOrCreateCell("V").value = strPayDetail
              row.getOrCreateCell("W").value = strPayStandard
              row.getOrCreateCell("X").value = accountYearMonth
              row.getOrCreateCell("Y").value = bankCardTime
              row.getOrCreateCell("Z").value = strAdjustTimeByCert
              row.getOrCreateCell("AA").value = strAdjustTimeByAudit
              row.getOrCreateCell("AB").value = strAdjustTimeByStop
              row.getOrCreateCell("AC").value = memo

              println(
                s"${i + 1} $memo $idCard $name $cbState $startTreatmentTime $endTreatmentTime " +
                  s"$payAmount $payCount $accountYearMonth $bankCardTime $strAdjustTimeByCert " +
                  s"$strAdjustTimeByAudit $strAdjustTimeByStop $strPayDetail $strPayStandard"
              )
            }
          }
        } finally {
          workbook.save(inputFile().insertBeforeLast(".upd"))
        }
      }
    }

  val statics =
    new Subcommand("statics") with InputFile with RowRange {
      descr("根据行政区划进行统计")

      val divisionRow = trailArg[String](
        descr = "行政区划所在列",
        required = false,
        default = Some("A")
      )

      def execute(): Unit = {
        val workbook = Excel.load(inputFile())
        val sheet = workbook.getSheetAt(0)

        import yhsb.cjb.net.protocol.Division._

        val groups = (for (index <- (startRow() - 1) until endRow())
          yield (sheet.getRow(index)(divisionRow()).value, index))
          .groupByDwAndCsName()

        var total = 0

        for ((dw, groups) <- groups) {
          val count = groups.values.foldLeft(0)(_ + _.size)
          println(s"\r\n${(dw + ":").padRight(11)}   ${count}")
          total += count
          for ((cs, indexes) <- groups) {
            println(s"  ${(cs + ":").padRight(11)} ${indexes.size}")
          }
        }
        println(s"\r\n${"合计:".padRight(11)} $total")
      }
    }

  val payState =
    new Subcommand("payState") with InputFile with RowRange {
      descr("查询参保人员待遇情况")

      val endDate = trailArg[String](descr = "截止日期, 如: 20210531")
      val nameCol = trailArg[String](descr = "姓名列")
      val idCardCol = trailArg[String](descr = "身份证列")
      val memoCol = trailArg[String](descr = "备注列")

      def execute(): Unit = {
        val month = endDate().substring(0, 6)

        lazy val payFailedMap = Session.use("006") { session =>
          session
            .request(PayListQuery(month, PayState.Wait))
            .filter(_.objectType == "1")
            .flatMap(it => session.request(PayListPersonalDetailQuery(it)))
            .filter(_.idCard.nonNullAndEmpty)
            .map(e => (e.idCard, e.name))
            .toMap
        }

        val workbook = Excel.load(inputFile())
        val sheet = workbook.getSheetAt(0)

        Session.use() { session =>
          for (index <- (startRow() - 1) until endRow()) {
            val row = sheet.getRow(index)
            val name = row(nameCol()).value
            var idCard = row(idCardCol()).value.toUpperCase.trim
            var memo = ""
            val len = idCard.length()
            if (len < 18) {
              memo = "身份证号码有误"
            } else {
              if (len > 18) idCard = idCard.substring(0, 18)
              session
                .request(PersonInfoInProvinceQuery(idCard))
                .headOption match {
                case None => memo = "未参加城居保"
                case Some(item) => {
                  if (item.agency != "湘潭市雨湖区") {
                    memo = s"未在我区参保(${item.agency})"
                  } else {
                    item.jbState match {
                      case "正常缴费" =>
                        session
                          .request(
                            RetiringPersonQuery(
                              retireDate = Formatter.toDashedDate(endDate()),
                              idCard = idCard,
                              inArrear = "",
                              cbState = ""
                            )
                          )
                          .headOption match {
                          case None => memo = "未达到享待年龄"
                          case Some(item) => {
                            if (item.memo.contains("欠费")) {
                              memo = "因欠费无法享待"
                            } else {
                              memo = "未知原因的正常缴费人员"
                            }
                          }
                        }
                      case "正常待遇" =>
                        if (
                          session
                            .request(
                              PaymentQuery(
                                startYearMonth = month,
                                idCard = idCard
                              )
                            )
                            .head
                            .amount > 0
                        ) {
                          memo = s"已支付到${month}"
                        } else if (payFailedMap.contains(idCard)) {
                          memo = "银行代发支付失败"
                        } else {
                          val item = session
                            .request(CertedPersonQuery(idCard))
                            .headOption
                          if (
                            item.isDefined && YearMonth.from(
                              item.get.certedDate.toInt
                            ) >= YearMonth.from(month.toInt)
                          ) {
                            memo = "补认证人员本月补发"
                          } else {
                            memo = "未知原因的正常待遇人员"
                          }
                        }
                      case s => memo = s"不可发放($s)"
                    }
                  }
                }
              }
            }
            println(s"$index $idCard $name $memo")
            row.getOrCreateCell(memoCol()).value = memo
          }
        }
        workbook.save(inputFile().insertBeforeLast(".up"))
      }
    }

  val paySum =
    new Subcommand("paySum") {
      descr("按年龄阶段统计特定月份发放金额")

      val month = trailArg[Int](descr = "发放月份, 例如: 202101")

      val range = Seq(
        60 -> 64,
        65 -> 74,
        75 -> Int.MaxValue
      )

      def execute(): Unit = {
        println(s"开始下载${month()}发放数据")

        val exportFile = Files.createTempFile("yhsb", ".xls").toString
        Session.use() {
          _.exportAllTo(
            PaymentQuery(
              f"${month()}%06d",
              f"${month()}%06d"
            ),
            PaymentQuery.columnMap
          )(
            exportFile
          )
        }

        println("开始统计发放数据")

        val result = range.map(_ => (0, BigDecimal(0))).to(ArrayBuffer)
        val workbook = Excel.load(exportFile)
        val sheet = workbook.getSheetAt(0)
        for (row <- sheet.rowIterator(1)) {
          val idCard = row("B").value
          if (idCard.length() == 18) {
            val amount = BigDecimal(row("H").value)
            val birthMonth = idCard.substring(6, 12).toInt
            val thisMonth = month().toInt

            val old =
              (((thisMonth / 100) * 12 + (thisMonth % 100)) - ((birthMonth / 100) * 12 + (birthMonth % 100))) / 12
            var continue = true
            for (((l, u), i) <- range.zipWithIndex if continue) {
              if (old >= l && old <= u) {
                result(i) = (result(i)._1 + 1, result(i)._2 + amount)
                continue = false
              }
            }
          }
        }

        println(result.map(r => f"${r._1} ${r._2}%.2f").mkString(" | "))
      }
    }

  val payment =
    new Subcommand("payment") {
      descr("按月统计待遇发放情况")

      val startMonth = trailArg[Int](descr = "开始月份, 如: 202001")
      val endMonth = trailArg[Int](descr = "结束行, 如: 202001")

      def execute(): Unit = {
        Session.use() { session =>
          for (
            ym <- YearMonthRange(
              YearMonth.from(startMonth()),
              YearMonth.from(endMonth())
            )
          ) {
            val total = session
              .request(PaymentQuery(ym.toString()))
              .last
            val totalOfPeople = total.idCard.toInt
            val totalOfMoney = total.amount
            val average = totalOfMoney / totalOfPeople
            println(f"$ym ${totalOfPeople} ${totalOfMoney}%.2f ${average}%.2f")
          }
        }
      }
    }

  val payAvg =
    new Subcommand("payAvg") {
      descr("按月统计待遇发放人数、金额、均值")

      val startMonth = trailArg[Int](descr = "开始月份, 如: 202001")
      val endMonth = trailArg[Int](descr = "结束行, 如: 202001")

      def execute(): Unit = {
        var (totalCount, totalSum) = (0, BigDecimal(0))
        Session.use() { session =>
          for (
            ym <- YearMonthRange(
              YearMonth.from(startMonth()),
              YearMonth.from(endMonth())
            )
          ) {
            println(s"开始下载${ym}发放数据")

            val exportFile = Files.createTempFile("yhsb", ".xls").toString

            session.exportAllTo(
              PaymentQuery(ym.toString),
              PaymentQuery.columnMap
            )(
              exportFile
            )

            println(s"开始统计${ym}发放数据")

            var (count, sum) = (0, BigDecimal(0))
            val workbook = Excel.load(exportFile)
            val sheet = workbook.getSheetAt(0)
            for (row <- sheet.rowIterator(1)) {
              val idCard = row("B").value
              val centralPersion = BigDecimal(row("I").value)
              if (idCard.length() == 18 && centralPersion > 0) {
                val amount = BigDecimal(row("H").value)
                count += 1
                sum += amount
              }
            }

            println(f"${ym}: ${count} ${sum}%.2f ${sum / count}%.2f\n")
            totalCount += count
            totalSum += sum
          }
        }
        println(
          f"${startMonth()}-${endMonth()}:  ${totalCount} ${totalSum}%.2f ${if (totalCount > 0) totalSum / totalCount
          else BigDecimal(0)}%.2f"
        )
      }
    }

  val cert =
    new Subcommand("cert") with InputFile with RowRange {
      descr("查询认证情况")

      def execute(): Unit = {
        val workbook = Excel.load(inputFile())
        val sheet = workbook.getSheetAt(0)

        try {
          Session.use() { session =>
            for {
              i <- (startRow() - 1) until endRow()
              row = sheet.getRow(i)
              name = row("F").value.trim()
              idCard = row("E").value.trim()
            } {
              print(s"$i $idCard $name")

              session
                .request(PersonInfoQuery(idCard))
                .headOption match {
                case None =>
                case Some(it) =>
                  print(s" ${it.jbState}")
                  row.getOrCreateCell("G").value = it.jbState

                  session
                    .request(CertedPersonQuery(idCard))
                    .headOption match {
                    case None =>
                    case Some(it) =>
                      if (
                        it.certType == CertType.Phone || it.certType == CertType.AutoTerm
                      ) {
                        print(
                          s" ${it.certType} ${it.certedDate} ${it.certedOpTime}"
                        )
                        row.getOrCreateCell("H").value = it.certType.toString()
                        row.getOrCreateCell("I").value = it.certedDate
                        row.getOrCreateCell("J").value = it.certedOpTime
                      }
                  }
              }
              println()
            }
          }
        } finally {
          workbook.save(inputFile().insertBeforeLast(".upd"))
        }
      }
    }

  var inherit = new Subcommand("inherit") with InputFile with RowRange {
    descr("死亡继承数据比对")

    def execute(): Unit = {
      val workbook = Excel.load(inputFile())
      val sheet = workbook.getSheetAt(0)

      try {
        Session.use() { session =>
          for {
            i <- (startRow() - 1) until endRow()
            row = sheet.getRow(i)
            idCard = row("B").value.trim()
            deathYearMonth = row("D").value.trim().toInt
          } {
            print(s"${i + 2 - startRow()} $idCard ")
            session
              .request(PersonInfoQuery(idCard))
              .headOption match {
              case None =>
                print("未查到我区参保记录")
              case Some(it) =>
                print(s"${it.name} ${it.jbState} ")
                row.getOrCreateCell("C").value = it.name
                row.getOrCreateCell("E").value = it.czName
                row.getOrCreateCell("F").value = it.dwName
                row.getOrCreateCell("G").value = it.jbState

                session
                  .request(RetiredPersonPauseAuditQuery(idCard, "1"))
                  .headOption match {
                    case None =>
                    case Some(it) =>
                      row.getOrCreateCell("H").value = "暂停"
                      val pauseYearMonth = it.pauseYearMonth
                      row.getOrCreateCell("I").value = pauseYearMonth
                      row.getOrCreateCell("J").value = {
                        val deathYear = deathYearMonth / 100
                        val deathMonth = deathYearMonth % 100
                        val pauseYear = pauseYearMonth / 100
                        val pauseMonth = pauseYearMonth % 100

                        (pauseYear - deathYear) * 12 + pauseMonth - deathMonth - 1
                      }
                      row.getOrCreateCell("K").value = it.reason.toString
                      row.getOrCreateCell("L").value = it.memo
                  }
              
                session
                  .request(SuspectedDeathQuery(idCard))
                  .headOption match {
                    case None =>
                    case Some(it) =>
                      row.getOrCreateCell("M").value = it.deathDate
                      val suspectedDeathYearMonth = it.deathDate / 100
                      row.getOrCreateCell("N").value = {
                        val deathYear = deathYearMonth / 100
                        val deathMonth = deathYearMonth % 100
                        val suspectedDeathYear = suspectedDeathYearMonth / 100
                        val suspectedDeathMonth = suspectedDeathYearMonth % 100
                        (deathYear - suspectedDeathYear) * 12 + deathMonth - suspectedDeathMonth
                      }
                  }

                session
                  .request(DataCompareQuery(idCard))
                  .headOption match {
                    case None =>
                    case Some(it) =>
                      row.getOrCreateCell("O").value = it.zbState.toString()
                      row.getOrCreateCell("P").value = it.pensionDate
                  }
            }
            println()
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
  addSubCommand(payment)
  addSubCommand(delayPay)
  addSubCommand(statics)
  addSubCommand(payState)
  addSubCommand(paySum)
  addSubCommand(payAvg)
  addSubCommand(cert)
  addSubCommand(inherit)
}

object Main {
  def main(args: Array[String]) = new Query(args).runCommand()
}
