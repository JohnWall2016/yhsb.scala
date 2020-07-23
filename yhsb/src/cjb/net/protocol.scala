package yhsb.cjb.net.protocol

import yhsb.util.json.Jsonable
import yhsb.util.json.Json.JsonName
import yhsb.cjb.net.Request
import yhsb.util.json.JsonField

case class SysLogin(
    @JsonName("username") val userName: String,
    @JsonName("passwd") val password: String
) extends Request("syslogin")

case class CbxxRequest(
    @JsonName("aac002") val idcard: String
) extends Request("executeSncbxxConQ")

case class Cbxx(
    @JsonName("aac001") val pid: Int,
    @JsonName("aac002") val idcard: String,
    @JsonName("aac003") val name: String,
    @JsonName("aac006") val birthDay: String,
    @JsonName("aac008") val cbState: CBState,
    @JsonName("aac031") val jfState: JFState,
    @JsonName("aac049") val cbTime: Int,
    @JsonName("aac066") val jbKind: JBKind,
    @JsonName("aaa129") val agency: String,
    @JsonName("aae036") val opTime: String,
    @JsonName("aaf101") val xzqhCode: String,
    @JsonName("aaf102") val czName: String,
    @JsonName("aaf103") val csName: String
) extends Jsonable {}

class CBState extends JsonField {
    override def valueMap = {
        case "0" => "未参保"
        case "1" => "正常参保"
        case "2" => "暂停参保"
        case "4" => "终止参保"
    }
}

class JFState extends JsonField {
    override def valueMap = {
        case "1" => "参保缴费"
        case "2" => "暂停缴费"
        case "3" => "终止缴费"
    }
}

class  JBKind extends JsonField {
    override def valueMap = {
        case "011" => "普通参保人员"
        case "021" => "残一级"
        case "022" => "残二级"
        case "031" => "特困一级"
        case "051" => "贫困人口一级"
        case "061" => "低保对象一级"
        case "062" => "低保对象二级"
    }
}

