package yhsb.cjb.db

import io.getquill._

case class LBTable1(
  idCard: String,
  name: String,
  address: String,
  bankName: String,
  cardNumber: String,
  dataType: String,
  reserve1: String,
  reserve2: String,
  reserve3: String,
)

trait CardsData { this: MysqlJdbcContext[_] =>
  val cardData = quote {
    querySchema[LBTable1](
      "cards_data",
      _.idCard -> "idcard",
      _.name -> "name",
      _.address -> "address",
      _.bankName -> "bank_name",
      _.cardNumber -> "card_number",
      _.dataType -> "data_type",
      _.reserve1 -> "reserve1",
      _.reserve2 -> "reserve2",
      _.reserve3 -> "reserve3",
    )
  }
}

trait JbData { this: MysqlJdbcContext[_] =>
  val jbData = quote {
    querySchema[LBTable1](
      "jb_data",
      _.idCard -> "idcard",
      _.name -> "name",
      _.address -> "address",
      _.bankName -> "bank_name",
      _.cardNumber -> "card_number",
      _.dataType -> "data_type",
      _.reserve1 -> "reserve1",
      _.reserve2 -> "reserve2",
      _.reserve3 -> "reserve3",
    )
  }
}

trait QmcbData { this: MysqlJdbcContext[_] =>
  val qmcbData = quote {
    querySchema[LBTable1](
      "qmcb_data",
      _.idCard -> "idcard",
      _.name -> "name",
      _.address -> "address",
      _.bankName -> "bank_name",
      _.cardNumber -> "card_number",
      _.dataType -> "data_type",
      _.reserve1 -> "reserve1",
      _.reserve2 -> "reserve2",
      _.reserve3 -> "reserve3",
    )
  }
}

object Lookback2021
  extends MysqlJdbcContext(LowerCase, "lookback2021")
     with CardsData
     with JbData
     with QmcbData