package yhsb.cjb.net.protocol

case class SysLogin(
    @JsonName("username") userName: String,
    @JsonName("passwd") password: String
) extends Request("syslogin")

object SysLogin {
  case class Item(
    @JsonName("username")
    userName: String,
    @JsonName("passwd")
    password: String,
  )
}