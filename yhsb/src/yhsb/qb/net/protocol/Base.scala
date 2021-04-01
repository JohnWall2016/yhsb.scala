package yhsb.qb.net.protocol

import yhsb.base.xml._

class Parameters(
  @transient val funID: String
)

@Namespaces(
  "soap" -> "http://schemas.xmlsoap.org/soap/envelope/"
)
@Node("soap:Envelope")
case class InEnvelope[T <: Parameters](
  params: T
)