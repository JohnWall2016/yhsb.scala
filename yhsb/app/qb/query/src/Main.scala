import yhsb.base.command._
import yhsb.base.excel.Excel._
import yhsb.base.text.String._
import yhsb.qb.net.Session
import yhsb.qb.net.protocol.AgencyCodeQuery
import yhsb.qb.net.protocol.CompanyInfoQuery
import yhsb.qb.net.protocol.JoinedPersonInProvinceQuery
import yhsb.qb.net.protocol.RetiredPersonQuery

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

              println(f"${index + 1}%-3d $code $name")

              row(codeCol()).setBlank()
              row(titleCol()).value = s"$name\n$title"
            }
          }
        } finally {
          workbook.save(inputFile().insertBeforeLast(".up"))
        }
      }
    }

  val retired =
    new Subcommand("retired") {
      descr("退休人员信息查询")

      val idCard = trailArg[String](descr = "身份证号码")

      def execute() = {
        Session.use() { session =>
          val result = session.request(AgencyCodeQuery())

          def getAgencyCode(name: String) =
            result.resultSet.find(_.name == name).get.code

          session
            .request(
              JoinedPersonInProvinceQuery(idCard())
            )
            .resultSet
            .foreach { it =>
              println(it)

              session
                .request(
                  RetiredPersonQuery(
                    it.idCard,
                    getAgencyCode(it.agencyName)
                  )
                )
                .resultSet
                .foreach(println)
            }
        }
      }
    }

  addSubCommand(doc)
  addSubCommand(retired)
}
