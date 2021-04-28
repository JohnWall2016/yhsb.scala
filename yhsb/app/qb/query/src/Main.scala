import yhsb.base.command._
import yhsb.base.excel.Excel._
import yhsb.base.text.String._

import yhsb.qb.net.Session
import yhsb.qb.net.protocol.CompanyInfoQuery

object Main {
  def main(args: Array[String]) = new Query(args).runCommand()
}

class Query(args: collection.Seq[String]) extends Command(args) {
  banner("数据查询处理程序")

  val doc =
    new Subcommand("doc") with InputFile with SheetName with RowRange {
      descr("档案目录生成")

      val codeCol = trailArg[String](descr = "单位编码列")
      val titleCol = trailArg[String](descr = "目录条目列")

      def execute() = {
        val workbook = Excel.load(inputFile())
        val sheet = workbook.getSheet(sheetName())

        try {
          Session.use() { session =>
            for (index <- (startRow() - 1) until endRow()) {
              val row = sheet.getRow(index)
              val code = row(codeCol()).value.trim()
              val title = row(titleCol()).value.trim()

              val name = session
                .request(CompanyInfoQuery(code))
                .resultSet
                .headOption
                .map(_.companyName)
                .getOrElse("")

              println(f"${index+1}%-3d $code $name")

              row(codeCol()).setBlank()
              row(titleCol()).value = s"$name\n$title"
            }
          }
        } finally {
          workbook.save(inputFile().insertBeforeLast(".up"))
        }
      }
    }

  addSubCommand(doc)
}
