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

trait UnionData { this: MysqlJdbcContext[_] =>
  val unionData = quote {
    querySchema[LBTable1](
      "union_data",
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

trait PoliceData { this: MysqlJdbcContext[_] =>
  val policeData = quote {
    querySchema[LBTable1](
      "police_data",
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

trait Table1Data { this: MysqlJdbcContext[_] =>
  val table1Data = quote {
    querySchema[LBTable1](
      "table1_data",
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

case class RetiredTable(
  idCard: String,
  name: String,
  areaCode: String,
  address: String,
  bankName: String,
  cardNumber: String,
  cardType: String,
  retiredType: String,
  stopTime: String,
  phone: String,
  oldAddress: String,
)

trait RetiredData { this: MysqlJdbcContext[_] =>
  val retiredData = quote {
    querySchema[RetiredTable](
      "retired_data",
      _.idCard -> "idcard",
      _.name -> "name",
      _.address -> "address",
      _.bankName -> "bank_name",
      _.cardNumber -> "card_number",
      _.areaCode -> "area_code",
      _.cardType -> "card_type",
      _.retiredType -> "retired_type",
      _.stopTime -> "stop_time",
      _.phone -> "phone",
      _.oldAddress -> "old_address"
    )
  }
}

case class VerifiedTable(
  idCard: String,
  name: String,
  dataType: String,
  reserve1: String,
  reserve2: String,
  reserve3: String,
)

trait FullCoverData { this: MysqlJdbcContext[_] =>
  val fullcoverData = quote {
    querySchema[VerifiedTable](
      "fullcover_data",
      _.idCard -> "idcard",
      _.name -> "name",
      _.dataType -> "data_type",
      _.reserve1 -> "reserve1",
      _.reserve2 -> "reserve2",
      _.reserve3 -> "reserve3",
    )
  }
}

trait CollegeStudentData { this: MysqlJdbcContext[_] =>
  val collegeStudentData = quote {
    querySchema[VerifiedTable](
      "college_student_data",
      _.idCard -> "idcard",
      _.name -> "name",
      _.dataType -> "data_type",
      _.reserve1 -> "reserve1",
      _.reserve2 -> "reserve2",
      _.reserve3 -> "reserve3",
    )
  }
}

trait VerifiedData { this: MysqlJdbcContext[_] =>
  val verifiedData = quote {
    querySchema[VerifiedTable](
      "verified_data",
      _.idCard -> "idcard",
      _.name -> "name",
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
     with UnionData
     with RetiredData
     with PoliceData
     with Table1Data
     with FullCoverData
     with CollegeStudentData
     with VerifiedData