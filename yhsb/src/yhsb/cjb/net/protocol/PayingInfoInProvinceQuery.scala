package yhsb.cjb.net.protocol

import scala.collection.mutable

import yhsb.base.math.Number.OptionBigDecimalOps
import yhsb.base.util.OptionalOps

/** 省内参保信息查询 - 缴费信息 */
case class PayingInfoInProvinceQuery(
    @JsonName("aac002") idcard: String
) extends PageRequest[PayingInfoInProvinceQuery.Item](
    "executeSncbqkcxjfxxQ",
    pageSize = 500
  )

object PayingInfoInProvinceQuery {
  case class Item(
      @JsonName("aae003") year: Int, // 缴费年度
      @JsonName("aae013") memo: String, // 备注
      @JsonName("aae022") amount: JBigDecimal, // 金额
      @JsonName("aaa115") typ: JFType, // 缴费类型
      @JsonName("aae341") item: JFItem, // 缴费项目
      @JsonName("aab033") method: JFMethod, // 缴费方式
      @JsonName("aae006") payedOffDay: String, // 划拨日期
      @JsonName("aaa027") agency: String, // 社保机构
      @JsonName("aaf101") xzqhCode: String // 行政区划代码
  ) extends Jsonable {
    def isPayedOff = payedOffDay != null
  }

  class PayInfoRecord(
      val year: Int, // 年度
      var personal: Option[BigDecimal] = None, // 个人缴费
      var provincial: Option[BigDecimal] = None, // 省级补贴
      var civic: Option[BigDecimal] = None, // 市级补贴
      var prefectural: Option[BigDecimal] = None, // 县级补贴
      var governmentalPay: Option[BigDecimal] = None, // 政府代缴
      var communalPay: Option[BigDecimal] = None, // 集体补助
      var fishmanPay: Option[BigDecimal] = None, // 退捕渔民补助
      val transferDates: mutable.Set[String] = mutable.Set(),
      val agencies: mutable.Set[String] = mutable.Set()
  )

  class PayInfoTotalRecord(
      var total: Option[BigDecimal] = None
  ) extends PayInfoRecord(0)


  implicit class RichItemResult(result: Result[Item]) {
    def getPayInfoRecordMap(): (
        collection.Map[Int, PayInfoRecord],
        collection.Map[Int, PayInfoRecord]
    ) = {
      val payedRecordMap = mutable.Map[Int, PayInfoRecord]()
      val unpayedRecordMap = mutable.Map[Int, PayInfoRecord]()

      for (data <- result) {
        if (data.year != 0) {
          val records =
            if (data.isPayedOff) payedRecordMap else unpayedRecordMap
          if (!records.contains(data.year)) {
            records(data.year) = new PayInfoRecord(data.year)
          }
          val record = records(data.year)
          data.item.value match {
            case "1"  => record.personal += data.amount
            case "3"  => record.provincial += data.amount
            case "4"  => record.civic += data.amount
            case "5"  => record.prefectural += data.amount
            case "6"  => record.communalPay += data.amount
            case "11" => record.governmentalPay += data.amount
            case _    => println(s"未知缴费类型${data.item.value}, 金额${data.amount}")
          }
          record.agencies.add(data.agency ?: "")
          record.transferDates.add(data.payedOffDay ?: "")
        }
      }

      (payedRecordMap, unpayedRecordMap)
    }

    def getPayInfoRecords()
        : (collection.Seq[PayInfoRecord], collection.Seq[PayInfoRecord]) = {
      val (payedRecords, unpayedRecords) = getPayInfoRecordMap()
      (orderAndTotal(payedRecords), orderAndTotal(unpayedRecords))
    }

    private def orderAndTotal(records: collection.Map[Int, PayInfoRecord]) = {
      var results = mutable.ListBuffer.from(records.values)
      results = results.sortWith((a, b) => a.year < b.year)
      val total = new PayInfoTotalRecord
      for (r <- results) {
        total.personal += r.personal
        total.provincial += r.provincial
        total.civic += r.civic
        total.prefectural += r.prefectural
        total.governmentalPay += r.governmentalPay
        total.communalPay += r.communalPay
        total.fishmanPay += r.fishmanPay
      }
      total.total = total.personal + total.provincial + total.civic +
        total.prefectural + total.governmentalPay + total.communalPay +
        total.fishmanPay
      results.addOne(total)
    }
  }
}
