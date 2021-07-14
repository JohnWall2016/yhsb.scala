package yhsb.cjb.db

import io.getquill._
import yhsb.base.text.String._
import io.getquill.ast.Entity

case class CjbSession(
    user: String,
    cxcookie: Option[String],
    jsessionid: Option[String],
)

trait CjbSessionData { this: SqliteJdbcContext[_] =>
  val cjbSessionData = quote {
    querySchema[CjbSession](
      "cjb_session",
      _.user -> "user",
      _.cxcookie -> "cxcookie",
      _.jsessionid -> "jsessionid_ylzcbp",
    )
  }
}

object YhsbDB
  extends SqliteJdbcContext(LowerCase, "yhsb.db")
     with CjbSessionData