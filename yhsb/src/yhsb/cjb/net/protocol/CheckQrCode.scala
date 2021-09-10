package yhsb.cjb.net.protocol

case class CheckQrCode(val qrCode: String, val qrCodeId: String)
  extends Request[String]("checkQrCode")
