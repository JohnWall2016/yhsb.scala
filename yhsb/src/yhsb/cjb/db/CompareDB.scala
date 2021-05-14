package yhsb.cjb.db

import io.getquill._
import yhsb.base.text.String._
import io.getquill.ast.Entity

case class Jbrymx(
    var idCard: String,
    /** 行政区划 */
    division: Option[String],
    /** 户籍性质 */
    familialType: Option[String],
    name: Option[String],
    /** 性别 */
    sex: Option[String],
    birthDay: Option[String],
    /** 参保身份 */
    jbKind: Option[String],
    /** 参保状态 */
    cbState: Option[String],
    /** 缴费状态 */
    jfState: Option[String],
    /** 参保时间 */
    joinedTime: Option[String],
)

trait JbrymxData { this: MysqlJdbcContext[_] =>
  val jbrymxData = quote {
    querySchema[Jbrymx](
      "jbrymx",
      _.idCard -> "idcard",
      _.division -> "xzqh",
      _.familialType -> "hjxz",
      _.name -> "name",
      _.sex -> "sex",
      _.birthDay -> "birthDay",
      _.jbKind -> "cbsf",
      _.cbState -> "cbzt",
      _.jfState -> "jfzt",
      _.joinedTime -> "cbsj",
    )
  }
}

case class Jgsbrymx(
    var idCard: String,
    name: Option[String],
    area: Option[String],
    memo: Option[String],
)

trait JgsbrymxData { this: MysqlJdbcContext[_] =>
  val jgsbrymxData = quote {
    querySchema[Jgsbrymx](
      "jgsbrymx",
      _.idCard -> "idcard",
      _.name -> "name",
      _.area -> "area",
      _.memo -> "memo",
    )
  }
}

case class Jgsbdup(
    var idCard: String,
    name: Option[String],
    division: Option[String],
    birthDay: Option[String],
    jbState: Option[String],
    jbKind: Option[String],
    jgsbName: Option[String],
    jgsbArea: Option[String],
)

trait JgsbdupData { this: MysqlJdbcContext[_] =>
  val JgsbdupData = quote {
    querySchema[Jgsbdup](
      "jgsbdup",
      _.idCard -> "idcard",
      _.name -> "name",
      _.division -> "xzqh",
      _.birthDay -> "birthday",
      _.jbState -> "jbzt",
      _.jbKind -> "jbsf",
      _.jgsbName -> "jgsb_name",
      _.jgsbArea -> "jgsb_area",
    )
  }
}

case class Table5(
    f1: String,
    f2: Option[String],
    f3: Option[String],
    f4: Option[String],
    f5: Option[String],
)

trait CustomTables { this: MysqlJdbcContext[_] =>
  val Table5_1 = quote {
    querySchema[Table5]("table5_1")
  }
  val Table5_2 = quote {
    querySchema[Table5]("table5_2")
  }
  val tableMap: PartialFunction[String, Quoted[EntityQuery[Table5]] { def quoted: Entity }] = {
    case "table5_1" => Table5_1
    case "table5_2" => Table5_2
  }

  trait TableQuoted[T] extends Quoted[EntityQuery[Table5]] {
    def quoted: Entity
  }

  def getTable(name: String) = tableMap(name)
}

object CompareData2021
  extends MysqlJdbcContext(LowerCase, "CompareDB2021")
     with JbrymxData
     with JgsbrymxData
     with JgsbdupData
     with CustomTables