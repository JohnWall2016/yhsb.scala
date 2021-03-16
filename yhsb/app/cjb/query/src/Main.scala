import yhsb.base.command._
import yhsb.base.excel.Excel
import yhsb.base.excel.Excel._
import yhsb.cjb.net.Session
import scala.collection.mutable
import yhsb.base.text.Strings._
import yhsb.base.io.PathOps._
import yhsb.cjb.net.protocol.PersonInfoInProvinceQuery
import yhsb.cjb.net.protocol.Result
import yhsb.cjb.net.protocol.PayingInfoInProvinceQuery
import yhsb.base.util.OptionalOps
import yhsb.base.util.UtilOps

class Query(args: Seq[String]) extends Command(args) {

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

            session.sendService(PersonInfoInProvinceQuery(idCard))
            val result = session.getResult[PersonInfoInProvinceQuery#Item]
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
      val neighborhoodRow = opt[String](name = "xzj", short = 'x', descr = "更新乡镇街列名称")
      val nameComparedRow = opt[String](name = "mzbd", short = 'm', descr = "更新姓名比对列名称")

      def execute(): Unit = {
        val workbook = Excel.load(inputFile())
        val sheet = workbook.getSheetAt(0)

        Session.use() { session =>
          for (i <- (startRow() - 1) until endRow()) {
            val row = sheet.getRow(i)
            val name = row.getCell(nameRow()).value.trim()
            val idCard = row.getCell(idCardRow()).value.trim().toUpperCase()

            println(idCard)

            session.sendService(PersonInfoInProvinceQuery(idCard))
            //println(session.readBody())
            val result = session.getResult[PersonInfoInProvinceQuery#Item]
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

      val idCard = trailArg[String](descr = "身份证号码")

      class PayInfoRecord(
        val year: Int, // 年度
        var personal: Option[BigDecimal] = None, // 个人缴费
        var provincial: Option[BigDecimal] = None, // 省级补贴
        var civic: Option[BigDecimal] = None, // 市级补贴
        var prefectural: Option[BigDecimal] = None, // 县级补贴
        var governmentalPay: Option[BigDecimal] = None, // 政府代缴
        var communalPay: Option[BigDecimal] = None, // 集体补助
        var fishmanPay: Option[BigDecimal] = None, // 退捕渔民补助
        val transferDates: mutable.Set[String] = mutable.Set(),
        val agencies: mutable.Set[String] = mutable.Set(),
      )

      class PayInfoTotalRecord(
        var total: Option[BigDecimal] = None
      ) extends PayInfoRecord(0)

      private implicit class OptionBigDecimalOps(left: Option[BigDecimal]) {
        def +(right: BigDecimal): Option[BigDecimal] =
          if (left.isDefined) {
            Some(left.get + right)
          } else {
            Some(right)
          }

        def +(right: Option[BigDecimal]): Option[BigDecimal] =
          if (left.isDefined) {
            if (right.isDefined) Some(left.get + right.get)
            else left
          } else {
            right
          }

        def mkString = left match {
          case Some(value) => value.toString()
          case None => "0"
        }

        def padLeft(width: Int) = mkString.padLeft(width)

        def padRight(width: Int) = mkString.padRight(width)
      }

      def getPayInfoRecords(
        payInfo: Result[PayingInfoInProvinceQuery#Item]
      ) = {
        val payedRecords = mutable.Map[Int, PayInfoRecord]()
        val unpayedRecords = mutable.Map[Int, PayInfoRecord]()

        for (data <- payInfo) {
          if (data.year != 0) {
            val records = if (data.isPayedOff) payedRecords else unpayedRecords
            if (!records.contains(data.year)) {
              records(data.year) = new PayInfoRecord(data.year)
            }
            val record = records(data.year)
            data.item.value match {
              case "1" => record.personal += data.amount
              case "3" => record.provincial += data.amount
              case "4" => record.civic += data.amount
              case "5" => record.prefectural += data.amount
              case "6" => record.communalPay += data.amount
              case "11" => record.governmentalPay += data.amount
              case _ => println(s"未知缴费类型${data.item.value}, 金额${data.amount}")
            }
            record.agencies.add(data.agency ?: "")
            record.transferDates.add(data.payedOffDay ?: "")
          }
        }

        (payedRecords, unpayedRecords)
      }

      def orderAndTotal(records: collection.Map[Int, PayInfoRecord]) = {
        var results = mutable.ListBuffer.from(records.values)
        results = results.sortWith((a, b) => a.year < b.year)
        val total = new PayInfoTotalRecord
        for (r <- results) {
          total.personal += r.personal
          total.provincial += r.provincial
          total.civic += r.civic
          total.prefectural += r.prefectural
          total.governmentalPay += r.governmentalPay
          total.communalPay += r.communalPay
          total.fishmanPay += r.fishmanPay
        }
        total.total =
          total.personal + total.provincial + total.civic +
            total.prefectural + total.governmentalPay + total.communalPay +
            total.fishmanPay
        results.addOne(total)
      }

      def printInfo(info: PersonInfoInProvinceQuery#Item) = {
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
          session.sendService(PersonInfoInProvinceQuery(idCard()))
          val info = session.getResult[PersonInfoInProvinceQuery#Item].let { result =>
            if (result.isEmpty || result(0).invalid) null else result(0)
          }

          session.sendService(PayingInfoInProvinceQuery(idCard()))
          val payInfoResult = session.getResult[PayingInfoInProvinceQuery#Item].let { result =>
            if (result.isEmpty || result.size == 1 && result(0).year == 0) null else result
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
          val (payedRecords, unpayedRecords) = getPayInfoRecords(payInfoResult)

          val records = orderAndTotal(payedRecords)
          val unrecords = orderAndTotal(unpayedRecords)

          printPayInfoRecords(records, "已拨付缴费历史记录:")
          if (unpayedRecords.nonEmpty) {
            printPayInfoRecords(unrecords, "\n未拨付补录入记录:")
          }

          (records, unrecords)
        }

        if (export() && records != null && records.nonEmpty) {
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
                case _ => cell.value = currentRow - startRow
              }
            }

            row("B").let { cell =>
              r match {
                case _: PayInfoTotalRecord => cell.value = "合计"
                case _ => cell.value = r.year
              }
            }

            row("C").value = r.personal.getOrElse(BigDecimal(0))
            row("D").value = r.provincial.getOrElse(BigDecimal(0))
            row("E").value = r.civic.getOrElse(BigDecimal(0))
            row("F").value = r.prefectural.getOrElse(BigDecimal(0))
            row("G").value = r.governmentalPay.getOrElse(BigDecimal(0))
            row("H").value = r.communalPay.getOrElse(BigDecimal(0))
            row("I").value = r.fishmanPay.getOrElse(BigDecimal(0))
            row("J").value =
              r match {
                case _: PayInfoTotalRecord => "总计"
                case _ => r.agencies.mkString("|")
              }
            row("L").let { cell =>
              r match {
                case t: PayInfoTotalRecord => cell.value = t.total
                case _ => cell.value = r.transferDates.mkString("|")
              }
            }
          }
          (path / s"${info.name}缴费查询单.xlsx").let { path =>
            println(s"\n保存: $path")
            workbook.save(path)
          }
        }
      }
    }

  addSubCommand(doc)
  addSubCommand(up)
  addSubCommand(payInfo)
}

object Main {
  def main(args: Array[String]) = new Query(args).runCommand()
}
