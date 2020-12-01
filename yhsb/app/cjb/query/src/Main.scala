package yhsb.app.cjb.query

import yhsb.util.commands._
import yhsb.util.Excel
import yhsb.util.Excel._
import yhsb.cjb.net.Session
import yhsb.cjb.net.protocol.CbxxRequest
import yhsb.cjb.net.protocol.Cbxx
import yhsb.cjb.net.protocol.Jfxx
import yhsb.util.Files.appendToFileName
import yhsb.cjb.net.Result

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
        val grjf: Option[BigDecimal] = None,
        val sjbt: Option[BigDecimal] = None,
        val sqbt: Option[BigDecimal] = None,
        val xjbt: Option[BigDecimal] = None,
        val zfdj: Option[BigDecimal] = None,
        val hbrq: Set[String] = Set(),
        val sbjg: Set[String] = Set(),
      )

      class JfxxTotalRecord(
        year: Int,
        grjf: Option[BigDecimal],
        sjbt: Option[BigDecimal],
        sqbt: Option[BigDecimal],
        xjbt: Option[BigDecimal],
        zfdj: Option[BigDecimal],
        hbrq: Set[String] = Set(),
        sbjg: Set[String] = Set(),
        val total: BigDecimal = 0
      ) extends JfxxRecord(
        year, grjf, sjbt, sqbt, xjbt, zfdj, hbrq, sbjg
      )

      def getJfxxRecords(
        jfxx: Result[Jfxx],
        payedRecords: Map[Int, JfxxRecord],
        unpayedRecords: Map[Int, JfxxRecord]
      ) = {
        for (data <- jfxx) {
          if (data.year != 0) {
            var records = if (data.isPayedOff) payedRecords else unpayedRecords
            if (!records.contains(data.year)) {
              //records(data.year) = new JfxxRecord(data.year)
            }
          }
        }
      }

      override def execute(): Unit = ???
    }

  addSubCommand(doc)
  addSubCommand(up)
}

object Main {
  def main(args: Array[String]) = new Query(args).runCommand()
}
