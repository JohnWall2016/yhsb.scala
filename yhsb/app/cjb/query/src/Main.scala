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

  addSubCommand {
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
            if (!result.isEmpty) {
              val cbxx = result(0)
              println(cbxx.name)
              row.getOrCreateCell("E").setCellValue(
                s"${cbxx.name}$title"
              )
            }
          }
        }
        workbook.save(appendToFileName(inputFile(), ".upd"))
      }
    }
  }
}

object Main {
  def main(args: Array[String]) = new Query(args).runCommand()
}