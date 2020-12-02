package yhsb.app.cjb.query

import yhsb.util.commands._
import yhsb.util.Excel
import yhsb.util.Excel._
import yhsb.util.Optional._
import yhsb.cjb.net.Session
import yhsb.cjb.net.protocol.CbxxRequest
import yhsb.cjb.net.protocol.Cbxx
import yhsb.cjb.net.protocol.Jfxx
import yhsb.util.Files.appendToFileName
import yhsb.cjb.net.Result
import scala.collection.mutable

class Query(args: Seq[String]) extends Command(args) {

  banner("数据查询处理程序")

  val doc =
    new Subcommand("doc") with InputFile {
      descr("档案目录生成")

      def execute() = {
        val workbook = Excel.load(inputFile())
        val sheet = workbook.getSheetAt(0)

        Session.use() { session =>
          for (i <- 0 to sheet.getLastRowNum()) {
            val row = sheet.getRow(i)
            val idcard = row.getCell("A").value
            val title = row.getCell("D").value

            session.sendService(CbxxRequest(idcard))
            val result = session.getResult[Cbxx]()
            if (result.isEmpty || result(0).idcard == null) {
              System.err.println(s"Error: ${i + 1} $idcard")
              System.exit(-1)
            } else {
              val cbxx = result(0)
              println(s"${i + 1} ${cbxx.name}")

              row
                .getOrCreateCell("E")
                .setCellValue(
                  s"${cbxx.name}$title"
                )
            }
          }
        }
        workbook.save(appendToFileName(inputFile(), ".upd"))
      }
    }

  val up =
    new Subcommand("up") with InputFile with RowRange {
      descr("更新参保信息")

      val nameRow = trailArg[String](descr = "姓名列名称")
      val idcardRow = trailArg[String](descr = "身份证列名称")
      val updateRow = trailArg[String](descr = "更新列名称")
      val xzjRow = opt[String](name = "xzj", short = 'x', descr = "更新乡镇街列名称")
      val mzbdRow = opt[String](name = "mzbd", short = 'm', descr = "更新姓名比对列名称")

      def execute(): Unit = {
        val workbook = Excel.load(inputFile())
        val sheet = workbook.getSheetAt(0)

        Session.use() { session =>
          for (i <- (startRow() - 1) until endRow()) {
            val row = sheet.getRow(i)
            val name = row.getCell(nameRow()).value.trim()
            val idcard = row.getCell(idcardRow()).value.trim().toUpperCase()

            println(idcard)

            session.sendService(CbxxRequest(idcard))
            //println(session.readBody())
            val result = session.getResult[Cbxx]()
            result.map(cbxx => {
              row
                .getOrCreateCell(updateRow())
                .setCellValue(cbxx.jbState)
              if (xzjRow.isDefined) {
                row
                  .getOrCreateCell(xzjRow())
                  .setCellValue(cbxx.dwName.get)
              }
              if (mzbdRow.isDefined && cbxx.name != name) {
                row
                  .getOrCreateCell(mzbdRow())
                  .setCellValue(cbxx.name)
              }
            })
          }
        }
        workbook.save(appendToFileName(inputFile(), ".upd"))
      }
    }

  val jfxx =
    new Subcommand("jfxx") with Export {
      descr("缴费信息查询")

      val idcard = trailArg[String](descr = "身份证号码")

      class JfxxRecord(
        val year: Int,
        var grjf: Option[BigDecimal] = None,
        var sjbt: Option[BigDecimal] = None,
        var sqbt: Option[BigDecimal] = None,
        var xjbt: Option[BigDecimal] = None,
        var zfdj: Option[BigDecimal] = None,
        var jtbz: Option[BigDecimal] = None,
        val hbrq: mutable.Set[String] = mutable.Set(),
        val sbjg: mutable.Set[String] = mutable.Set(),
      )

      class JfxxTotalRecord(
        grjf: Option[BigDecimal] = None,
        sjbt: Option[BigDecimal] = None,
        sqbt: Option[BigDecimal] = None,
        xjbt: Option[BigDecimal] = None,
        zfdj: Option[BigDecimal] = None,
        jtbz: Option[BigDecimal] = None,
        var total: Option[BigDecimal] = None
      ) extends JfxxRecord(
        0, grjf, sjbt, sqbt, xjbt, zfdj, jtbz
      )

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
      }

      def getJfxxRecords(
        jfxx: Result[Jfxx],
        payedRecords: mutable.Map[Int, JfxxRecord],
        unpayedRecords: mutable.Map[Int, JfxxRecord]
      ) = {
        for (data <- jfxx) {
          if (data.year != 0) {
            var records = if (data.isPayedOff) payedRecords else unpayedRecords
            if (!records.contains(data.year)) {
              records(data.year) = new JfxxRecord(data.year)
            }
            val record = records(data.year)
            data.item.value match {
              case "1" => record.grjf += data.amount
              case "3" => record.sjbt += data.amount
              case "4" => record.sqbt += data.amount
              case "5" => record.xjbt += data.amount
              case "6" => record.jtbz += data.amount
              case "11" => record.zfdj += data.amount
              case _ => println(s"未知缴费类型${data.item.value}, 金额${data.amount}")
            }
            record.sbjg.add(data.agency ?: "")
            record.hbrq.add(data.payedOffDay ?: "")
          }
        }
      }

      def orderAndTotal(records: collection.Map[Int, JfxxRecord]) = {
        var results = mutable.ListBuffer.from(records.values)
        results = results.sortWith((a, b) => a.year < b.year)
        val total = new JfxxTotalRecord
        for (r <- results) {
          total.grjf += r.grjf
          total.sjbt += r.sjbt
          total.sqbt += r.sqbt
          total.xjbt += r.xjbt
          total.zfdj += r.zfdj
          total.jtbz += r.jtbz
        }
        total.total = 
          total.grjf + total.sjbt + total.sqbt
          total.xjbt + total.zfdj + total.jtbz
        results.addOne(total)
      }

      override def execute(): Unit = ???
    }

  addSubCommand(doc)
  addSubCommand(up)
}

object Main {
  def main(args: Array[String]) = new Query(args).runCommand()
}
