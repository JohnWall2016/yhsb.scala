import yhsb.base.command.Command
import yhsb.cjb.db.RawItem
import yhsb.cjb.db.AuthData2021
import yhsb.base.text.String._
import yhsb.cjb.db.AuthItem

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
          
          val data: List[AuthItem] = run(historyData.filter(_.idCard == lift(idCard)))
          if (data.nonEmpty) {
            
          }
        }
      }
    }
  }
}