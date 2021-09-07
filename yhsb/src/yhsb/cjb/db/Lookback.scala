package yhsb.cjb.db
package lookback

import io.getquill._

case class Table1(
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
    querySchema[Table1](
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
    querySchema[Table1](
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
    querySchema[Table1](
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
    querySchema[Table1](
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
    querySchema[Table1](
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
    querySchema[Table1](
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

trait Table1VerifiedData { this: MysqlJdbcContext[_] =>
  val table1VerifiedData = quote {
    querySchema[Table1](
      "table1_verified_data",
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

case class JbStopTable(
  idCard: String,
  name: String,
  dataType: String,
  stopTime: String,
  stopReason: String,
  memo: String,
  auditTime: String,
)

trait JbStopData { this: MysqlJdbcContext[_] =>
  val jbStopData = quote {
    querySchema[JbStopTable](
      "jb_retired_stop_data",
      _.idCard -> "idcard",
      _.name -> "name",
      _.dataType -> "data_type",
      _.stopTime -> "stop_time",
      _.stopReason -> "stop_reason",
      _.memo -> "memo",
      _.auditTime -> "audit_time",
    )
  }
}

case class SSCompareResult(
  idCard: String,
  name: String,
  dataType: String,
  resultName: String,
  resultArea: String, // 行政区划名称
  resultType: String, // 养老保险类型
  resultMemo: String,
)

trait SSCompareData { this: MysqlJdbcContext[_] =>
  val ssCompareData = quote {
    querySchema[SSCompareResult](
      "social_security_compare_result",
      _.idCard -> "idcard",
      _.name -> "name",
      _.dataType -> "data_type",
      _.resultName -> "result_name",
      _.resultArea -> "result_area",
      _.resultType -> "result_type",
      _.resultMemo -> "result_memo",
    )
  }
}

case class Table1CompareResult(
  idCard: String,
  name: String,
  address: String,
  bankName: String,
  cardNumber: String,
  dataType: String,
  reserve1: String,
  reserve2: String,
  reserve3: String,
  resultDataType: String,
  resultName: String,
  resultArea: String, // 行政区划名称
  resultType: String, // 养老保险类型
  resultMemo: String,
)

case class Table1VerifiedResult(
  idCard: String,
  name: String,
  address: String,
  bankName: String,
  cardNumber: String,
  dataType: String,
  reserve1: String,
  reserve2: String,
  reserve3: String,
  resultDataType: String,
  resultName: String,
  resultArea: String, // 行政区划名称
  resultType: String, // 养老保险类型
  resultMemo: String,
  verified: String,
)

case class Table2VerifiedResult(
  villageName: String,
  idCard: String,
  name: String,
  verified: String,
  bankAccount: String,
  bankName: String,
  cardType: String,
  payState: String,
  stopTime: String,
  phone: String,
  alive: String,
  keepCardBySelf: String,
  deathDate: String,
  keepCardByRelative: String,
  abnormalType: String,
  abnormalDetail: String,
  verifiedWay: String,
  verifiedPhone: String,
  verifier: String,
  verifierPhone: String,
)

case class OutsideDeathItem(
  idCard: String,
  name: String,
  dataType: String,
  deathDate: String,
)

trait Table1CompareData { this: MysqlJdbcContext[_] =>
  val table1CompareData = quote {
    querySchema[Table1CompareResult](
      "table1_data_with_compare_data",
      _.idCard -> "idcard",
      _.name -> "name",
      _.address -> "address",
      _.bankName -> "bank_name",
      _.cardNumber -> "card_number",
      _.dataType -> "data_type",
      _.reserve1 -> "reserve1",
      _.reserve2 -> "reserve2",
      _.reserve3 -> "reserve3",
      _.resultDataType -> "result_data_type",
      _.resultName -> "result_name",
      _.resultArea -> "result_area",
      _.resultType -> "result_type",
      _.resultMemo -> "result_memo",
    )
  }
}

trait Table1VerifiedAllData { this: MysqlJdbcContext[_] =>
  val table1VerifiedAllData = quote {
    querySchema[Table1VerifiedResult](
      "table1_all_verified_data",
      _.idCard -> "idcard",
      _.name -> "name",
      _.address -> "address",
      _.bankName -> "bank_name",
      _.cardNumber -> "card_number",
      _.dataType -> "data_type",
      _.reserve1 -> "reserve1",
      _.reserve2 -> "reserve2",
      _.reserve3 -> "reserve3",
      _.resultDataType -> "result_data_type",
      _.resultName -> "result_name",
      _.resultArea -> "result_area",
      _.resultType -> "result_type",
      _.resultMemo -> "result_memo",
      _.verified -> "verified",
    )
  }
}

trait Table2VerifiedData { this: MysqlJdbcContext[_] =>
  val table2VerifiedData = quote {
    querySchema[Table2VerifiedResult](
      "table2_verified_data",
      _.villageName -> "village_name",
      _.idCard -> "idcard",
      _.name -> "name",
      _.verified -> "verified",
      _.bankAccount -> "bank_account",
      _.bankName -> "bank_name",
      _.cardType -> "card_type",
      _.payState -> "pay_state",
      _.stopTime -> "stop_time",
      _.phone -> "phone",
      _.alive -> "alive",
      _.keepCardBySelf -> "keep_card_by_self",
      _.deathDate -> "death_date",
      _.keepCardByRelative -> "keep_card_by_relative",
      _.abnormalType -> "abnormal_type",
      _.abnormalDetail -> "abnormal_detail",
      _.verifiedWay -> "verified_way",
      _.verifiedPhone -> "verified_phone",
      _.verifier -> "verifier",
      _.verifierPhone -> "verifier_phone",
    )
  }
}

trait OutsideDeathData { this: MysqlJdbcContext[_] =>
  val outsideDeathData = quote {
    querySchema[OutsideDeathItem](
      "outside_death_data",
      _.idCard -> "idcard",
      _.name -> "name",
      _.dataType -> "data_type",
      _.deathDate -> "death_date",
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
     with JbStopData
     with SSCompareData
     with Table1CompareData
     with Table1VerifiedData
     with Table1VerifiedAllData
     with Table2VerifiedData
     with OutsideDeathData
     