import yhsb.base.command.Command
import yhsb.cjb.db.RawItem
import yhsb.cjb.db.AuthData2021
import yhsb.base.text.String._
import yhsb.cjb.db.AuthItem
import yhsb.cjb.db.HistoryItem
import yhsb.cjb.db.MonthItem
import io.getquill.Query
import yhsb.base.excel.Excel._
import org.rogach.scallop.ScallopConf
import yhsb.base.command.Subcommand
import yhsb.base.db.Context.JdbcContextOps

object Main {
  def main(args: Array[String]): Unit = new Auth(args).runCommand()
}

class Auth(args: collection.Seq[String]) extends Command(args) {
  banner("城居参保身份认证程序")

  addSubCommand(new ImportVeryPoor)
  addSubCommand(new ImportCityAllowance)
  addSubCommand(new ImportCountryAllowance)
  addSubCommand(new ImportDisability)
  addSubCommand(new MergeHistory)
  addSubCommand(new GenerateBook)
  addSubCommand(new Authenticate)
  addSubCommand(new ImportJBData)
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
    
    data.foreach { item =>
      val index = currentRow - startRow + 1

      println(s"$index ${item.idCard} ${item.name}")

      val row = sheet.getOrCopyRow(currentRow, startRow)
      currentRow += 1
      row("A").value = index
      row("B").value = item.no
      row("C").value = item.neighborhood
      row("D").value = item.community
      row("E").value = item.address
      row("F").value = item.name
      row("G").value = item.idCard
      row("H").value = item.birthDay
      row("I").value = item.poverty
      row("J").value = item.povertyDate
      row("K").value = item.veryPoor
      row("L").value = item.veryPoorDate
      row("M").value = item.fullAllowance
      row("N").value = item.fullAllowanceDate
      row("O").value = item.shortAllowance
      row("P").value = item.shortAllowanceDate
      row("Q").value = item.primaryDisability
      row("R").value = item.primaryDisabilityDate
      row("S").value = item.secondaryDisability
      row("T").value = item.secondaryDisabilityDate
      row("U").value = item.isDestitute
      row("V").value = item.jbKind
      row("W").value = item.jbKindFirstDate
      row("X").value = item.jbKindLastDate
      row("Y").value = item.jbState
      row("Z").value = item.jbStateDate
      row("AA").setBlank()
    }

    workbook.save(file)
    println(s"结束导出底册: $file")
  }
}

trait ImportCommand {  _: ScallopConf =>
  val date = trailArg[String](descr = "数据月份, 例如: 201912")
  val excel = trailArg[String](descr = "excel表格文件路径")
  val startRow = trailArg[Int](descr = "开始行, 从1开始")
  val endRow = trailArg[Int](descr = "结束行, 包含在内")
  val clear = opt[Boolean](descr = "是否清除已有数据", default = Some(false))
  val clearOnly = opt[Boolean](descr = "只清除已有数据", default = Some(false))

  def clearData(): Unit

  case class FieldColumns(
    nameAndIdCards: Seq[(String, String)],
    neighborhood: String,
    community: String,
    address: Option[String] = None,
    personType: Option[String] = None
  )(
    val init: RawItem => Unit
  )

  val fieldColumns: FieldColumns

  private def fetchData(): Iterable[RawItem] = {
    require(
      startRow() > 0 && startRow() <= endRow(),
      "开始行必须大于0, 且不能大于结束行"
    )

    val workbook = Excel.load(excel())
    val sheet = workbook.getSheetAt(0)

    for {
      index <- (startRow() - 1) to endRow()
      row = sheet.getRow(index)
      neighborhood = row(fieldColumns.neighborhood).value
      community = row(fieldColumns.community).value
      personType = fieldColumns.personType.map(row(_).value).getOrElse("")
      address = fieldColumns.address.map(row(_).value).getOrElse("")
      (nameCol, idCardCol) <- fieldColumns.nameAndIdCards
      name = row(nameCol).value.trim()
      idCard = row(idCardCol).value.trim()
      len = idCard.length()
      if (name.nonEmpty && idCard.nonEmpty && len >= 18)
    } yield {
      val item = RawItem(
        neighborhood = Some(neighborhood),
        community = Some(community),
        address = Some(address),
        name = Some(name),
        idCard = if (len > 18) idCard.substring(0, 18) else idCard,
        birthDay = Some(idCard.substring(6, 14)),
        date = Some(date()),
        personType = Some(personType)
      )
      fieldColumns.init(item)
      item
    }
  }

  def execute(): Unit = {
    if (clear() || clearOnly()) {
      println("开始清除已有数据")
      clearData()
      println("结束清除已有数据")
    }
    if (!clearOnly()) {
      Auth.importRawData(fetchData())
    }
  }
}

class ImportVeryPoor extends Subcommand("tkry") with ImportCommand {
  descr("导入特困人员数据")

  override def clearData(): Unit = {
    import AuthData2021._
    run(
      rawData.filter(it => 
        it.date == Option(lift(date())) && it.personType == Some("特困人员")
      ).delete
    )
  }

  override val fieldColumns: FieldColumns = 
    FieldColumns(
      nameAndIdCards = Seq("G" -> "H"),
      neighborhood = "C",
      community = "D",
      address = Some("E"),
    ) { item =>
      item.personType = Some("特困人员")
      item.detail = Some("是")
    }
}

class ImportCityAllowance extends Subcommand("csdb") with ImportCommand {
  descr("导入城市低保数据")

  override def clearData(): Unit = {
    import AuthData2021._
    run(
      rawData.filter(it => 
        it.date == Option(lift(date())) && 
        (infix"type like '%低保人员'".as[Boolean]) && 
        it.detail == Some("城市")
      ).delete
    )
  }

  override val fieldColumns: FieldColumns = 
    FieldColumns(
      nameAndIdCards = Seq(
        "I" -> "J",
        "K" -> "L",
        "M" -> "N",
        "O" -> "P",
        "Q" -> "R",
      ),
      neighborhood = "A",
      community = "B",
      address = Some("E"),
      personType = Some("G"),
    ) { item =>
      item.personType = 
        item.personType match {
          case Some("全额救助") | Some("全额") => Some("全额低保人员")
          case None | Some(_) => Some("差额低保人员")
        }
      item.detail = Some("城市")
    }
}

class ImportCountryAllowance extends Subcommand("ncdb") with ImportCommand {
  descr("导入农村低保数据")

  override def clearData(): Unit = {
    import AuthData2021._
    run(
      rawData.filter(it => 
        it.date == Option(lift(date())) && 
        (infix"type like '%低保人员'".as[Boolean]) && 
        it.detail == Some("农村")
      ).delete
    )
  }

  override val fieldColumns: FieldColumns = 
    FieldColumns(
      nameAndIdCards = Seq(
        "H" -> "J",
        "K" -> "L",
        "M" -> "N",
        "O" -> "P",
        "Q" -> "R",
        "S" -> "T",
      ),
      neighborhood = "A",
      community = "B",
      address = Some("D"),
      personType = Some("F"),
    ) { item =>
      item.personType = 
        item.personType match {
          case Some("全额救助") | Some("全额") => Some("全额低保人员")
          case None | Some(_) => Some("差额低保人员")
        }
      item.detail = Some("农村")
    }
}

class ImportDisability extends Subcommand("cjry") with ImportCommand {
  descr("导入残疾人员数据")

  override def clearData(): Unit = {
    import AuthData2021._
    run(
      rawData.filter(it => 
        it.date == Option(lift(date())) && 
        (infix"type like '%残疾人员'".as[Boolean])
      ).delete
    )
  }

  override val fieldColumns: FieldColumns = 
    FieldColumns(
      nameAndIdCards = Seq(
        "A" -> "B",
      ),
      neighborhood = "E",
      community = "F",
      address = Some("G"),
      personType = Some("O"),
    ) { item =>
      item.personType = 
        item.personType match {
          case Some("一级") | Some("二级") => Some("一二级残疾人员")
          case Some("三级") | Some("四级") => Some("三四级残疾人员")
          case other => throw new Exception(s"未知残疾类型: $other")
        }
    }
}

class MergeHistory extends Subcommand("hbdc") {
  descr("合并历史数据底册")

  val date = trailArg[String](descr = "数据月份, 例如: 201912")

  override def execute(): Unit = {
    println("开始合并导入数据至: 特殊缴费类型历史数据底册")
    Auth.mergeRawData(date())
    println("结束合并导入数据至: 特殊缴费类型历史数据底册")
  }
}

class GenerateBook extends Subcommand("scdc") {
  descr("生成当月数据底册")

  val date = trailArg[String](descr = "数据月份, 例如: 201912")
  val clear = opt[Boolean](descr = "是否清除数据表")

  override def execute(): Unit = {
    import AuthData2021._

    if (clear()) {
      println(s"开始清除数据: ${date()}特殊缴费类型数据底册")
      run(monthData.filter(_.month == Some(lift(date()))).delete)
      println(s"结束清除数据: ${date()}特殊缴费类型数据底册")
    }

    println(s"开始合并导入数据至: ${date()}特殊缴费类型数据底册")
    Auth.mergeRawData(date(), date())
    println(s"结束合并导入数据至: ${date()}特殊缴费类型数据底册")
  }
}

class Authenticate extends Subcommand("rdsf") {
  descr("认定居保参保身份")

  val monthOrAll = trailArg[String](descr = "表格数据月份, 例如: 201912, ALL")
  val date = trailArg[String](descr = "认定月份, 例如: 201912")

  override def execute(): Unit = {
    println(s"开始认定参保人员身份: ${monthOrAll}特殊缴费类型数据底册")
    Auth.authenticate(date(), monthOrAll())
    println(s"结束认定参保人员身份: ${monthOrAll}特殊缴费类型数据底册")
  }
}

class ImportJBData extends Subcommand("drjb") {
  descr("导入居保参保数据")

  val excel = trailArg[String](descr = "excel表格文件路径")
  val startRow = trailArg[Int](descr = "开始行, 从1开始")
  val endRow = trailArg[Int](descr = "结束行, 包含在内")
  val clear = opt[Boolean](descr = "是否清除已有数据", default = Some(false))

  override def execute(): Unit = {
    import AuthData2021._

    if (clear()) {
      println("开始清除数据: 居保参保人员明细表")
      run(joinedPersonData.delete)
      println("结束清除数据: 居保参保人员明细表")
    }

    println("开始导入居保参保人员明细表")
    AuthData2021.loadExcel(
      joinedPersonData.quoted,
      excel(),
      startRow(),
      endRow(),
      Seq("E", "A", "B", "C", "F", "G", "I", "K", "L", "Q"),
      printSql = true
    )
    println("结束导入居保参保人员明细表")
  }
}