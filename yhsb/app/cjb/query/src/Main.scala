package yhsb.app.cjb.query

import yhsb.util.commands._
import yhsb.util.Excel
import yhsb.util.Excel._
import yhsb.cjb.net.Session
import yhsb.cjb.net.protocol.CbxxRequest
import yhsb.cjb.net.protocol.Cbxx
import yhsb.util.Files.appendToFileName

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

      val idcardRow = trailArg[String](descr = "身份证列名称")
      val updateRow = trailArg[String](descr = "更新列名称")

      def execute(): Unit = {
        val workbook = Excel.load(inputFile())
        val sheet = workbook.getSheetAt(0)

        Session.use() { session =>
          for (i <- (startRow() - 1) until endRow()) {
            val row = sheet.getRow(i)
            val idcard = row.getCell(idcardRow()).value.trim().toUpperCase()

            println(idcard)

            session.sendService(CbxxRequest(idcard))
            val result = session.getResult[Cbxx]()

            row
              .getOrCreateCell(updateRow())
              .setCellValue {
                if (result.isEmpty || result(0).idcard == null) {
                  ""
                } else {
                  result(0).jbState
                }
              }
          }
        }
        workbook.save(appendToFileName(inputFile(), ".upd"))
      }
    }

  addSubCommand(doc)
  addSubCommand(up)
}

object Main {
  def main(args: Array[String]) = new Query(args).runCommand()
}
