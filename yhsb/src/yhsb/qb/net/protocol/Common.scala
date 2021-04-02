package yhsb.qb.net.protocol

import yhsb.base.struct.MapField

/** 社保状态 */
class SBState extends MapField {
  override def valueMap = {
    case "1" => "在职"
    case "2" => "退休"
    case "3" => "终止" //?
    case "4" => "终止" //?
  }
}

/** 参保状态 */
class CBState extends MapField {
  override def valueMap = {
    case "1" => "参保缴费"
    case "2" => "暂停缴费"
    case "3" => "终止缴费"
  }
}

/** 缴费人员类别 */
class JFKind extends MapField {
  override def valueMap = {
    case "101" => "单位在业人员"
    case "102" => "个体缴费"
  }
}

/** 记账标志 */
class BookMark extends MapField {
  override def valueMap = {
    case "0" => "未记账"
    case "1" => "已记账"
  }
}