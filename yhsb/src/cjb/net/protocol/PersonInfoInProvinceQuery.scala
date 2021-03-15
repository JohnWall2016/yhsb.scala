package yhsb.cjb.net.protocol

/**
  * 省内参保信息查询
  *
  * @param idcard
  */
case class PersonInfoInProvinceQuery(
    @JsonName("aac002") idcard: String
) extends Request("executeSncbxxConQ") {
  case class Item(
      @JsonName("aac001") pid: Int,
      @JsonName("aac002") idcard: String,
      @JsonName("aac003") name: String,
      @JsonName("aac006") birthDay: String,
      @JsonName("aac008") cbState: CBState,
      @JsonName("aac031") jfState: JFState,
      @JsonName("aac049") cbTime: Int,
      @JsonName("aac066") jbKind: JBKind,
      @JsonName("aaa129") agency: String,
      @JsonName("aae036") opTime: String,
      //@JsonName("aaf101") xzqhCode: String,
      @JsonName("aaf102") czName: String
      //@JsonName("aaf103") csName: String
  ) extends Jsonable
      with JBState
      with DivisionName {
    
    def valid = idcard != null && !idcard.isEmpty()

    def invalid = !valid
  }
}

