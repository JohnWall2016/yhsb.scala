import yhsb.base.command.Command
import yhsb.cjb.db.RawItem
import yhsb.cjb.db.AuthData2021
import yhsb.base.text.String._
import yhsb.cjb.db.AuthItem
import yhsb.cjb.db.HistoryItem
import yhsb.cjb.db.MonthItem
import yhsb.base.excel.Excel
import io.getquill.Query

object Main {
  def main(args: Array[String]): Unit = new Auth(args).runCommand()
}

class Auth(args: collection.Seq[String]) extends Command(args) {
  banner("城居参保身份认证程序")


}

object Auth {
  def importRawData(items: Iterable[RawItem]) = {
    import AuthData2021._

    transaction {
      items.zipWithIndex
        .foreach { case (item, index) =>
          println(s"${index + 1} ${item.idCard} ${item.name.getOrElse("").padRight(6)} ${item.personType.getOrElse("")} ")
          if (item.idCard.nonNullAndEmpty) {
            val result: List[RawItem] = 
              run(
                rawData.filter { it =>
                  it.idCard == lift(item.idCard) &&
                  it.personType == lift(item.personType) &&
                  it.date == lift(item.date)
                }
              )
            if (result.nonEmpty) {
              result.foreach { it =>
                item.no = it.no
                run(
                  rawData.update(lift(item))
                )
              }
              println("更新")
            } else {
              run(
                rawData.insert(lift(item))
              )
              println("新增")
            }
          } else {
            println("数据无效")
          }
        }
    }
  }

  def mergeRawData(date: String, month: String = null) = {
    import AuthData2021._
    
    transaction {
      val list: List[RawItem] = run(
        rawData.filter(_.date == lift(Option(date)))
      )

      val groups = list.groupBy(_.idCard)

      if (month == null) {
        groups.zipWithIndex.foreach { case ((idCard, items), index) =>
          println(s"${index + 1} $idCard")
          
          val data: List[HistoryItem] =
            run(historyData.filter(_.idCard == lift(idCard)))
          if (data.nonEmpty) {
            data.foreach { item =>
              items.foreach(item.merge(_))
              run(historyData.update(lift(item)))
            }
          } else {
            val item = HistoryItem()
            items.foreach(item.merge(_))
            run(historyData.insert(lift(item)))
          }
        }
      } else {
        groups.zipWithIndex.foreach { case ((idCard, items), index) =>
          println(s"${index + 1} $idCard")
          
          val data: List[MonthItem] =
            run(monthData.filter(it => it.idCard == lift(idCard) && it.month == lift(Option(month))))
          if (data.nonEmpty) {
            data.foreach { item =>
              items.foreach(item.merge(_))
              run(monthData.update(lift(item)))
            }
          } else {
            val item = MonthItem(month = Some(month))
            items.foreach(item.merge(_))
            run(monthData.insert(lift(item)))
          }
        }
      }
    }
  }

  def authenticate(date: String, monthOrAll: String) = {
    import AuthData2021._
    
    transaction {
      val data: List[AuthItem] =
        if (monthOrAll.toUpperCase() == "ALL") {
          run(historyData)
        } else {
          run(monthData.filter(_.month == lift(Option(monthOrAll))))
        }

      var index = 1
      data.foreach { item =>
        var jbKind: String = null
        var isDestitute: String = null
        if (item.poverty.getOrElse("").nonEmpty) {
          jbKind = "贫困人口一级"
          isDestitute = "贫困人口"
        } else if (item.veryPoor.getOrElse("").nonEmpty) {
          jbKind = "特困一级"
          isDestitute = "特困人员"
        } else if (item.fullAllowance.getOrElse("").nonEmpty) {
          jbKind = "低保对象一级"
          isDestitute = "低保对象"
        } else if (item.primaryDisability.getOrElse("").nonEmpty) {
          jbKind = "残一级"
        } else if (item.shortAllowance.getOrElse("").nonEmpty) {
          jbKind = "低保对象二级"
          isDestitute = "低保对象"
        } else if (item.secondaryDisability.getOrElse("").nonEmpty) {
          jbKind = "残二级"
        }

        var update = false
        if (jbKind != null && jbKind != item.jbKind.getOrElse("")) {
          if (item.jbKind.getOrElse("").nonEmpty) {
            println(s"$index ${item.idCard} ${item.name.getOrElse("").padRight(6)} $jbKind <- ${item.jbKind}")
          } else {
            println(s"$index ${item.idCard} ${item.name.getOrElse("").padRight(6)} $jbKind")
          }
          item.jbKind = Some(jbKind)
          item.jbKindLastDate = Some(date)
          update = true
        }

        if (isDestitute != null && isDestitute != item.isDestitute.getOrElse("")) {
          item.isDestitute = Some(isDestitute)
          update = true
        }

        if (update) {
          if (monthOrAll.toUpperCase() == "ALL") {
            run(historyData.update(lift(item.asInstanceOf[HistoryItem])))
          } else {
            run(monthData.update(lift(item.asInstanceOf[MonthItem])))
          }
        }
      }
    }
  }

  def exportData(monthOrAll: String, template: String, file: String) = {
    println(s"开始导出底册: $file")

    val workbook = Excel.load(template)
    val sheet = workbook.getSheetAt(0)
    val startRow = 2
    var currentRow = startRow

    import AuthData2021._

    val data: List[AuthItem] =
      if (monthOrAll.toUpperCase() == "ALL") {
        run {
          quote(
            infix"$historyData ORDER BY CONVERT( xzj USING gbk ), CONVERT( csq USING gbk ), CONVERT( name USING gbk )".as[Query[HistoryItem]])
        }
      } else {
        run {
          quote(
            infix"$monthData where month='${lift(monthOrAll)}' ORDER BY CONVERT( xzj USING gbk ), CONVERT( csq USING gbk ), CONVERT( name USING gbk )".as[Query[MonthItem]])
        }
      }
  }
}