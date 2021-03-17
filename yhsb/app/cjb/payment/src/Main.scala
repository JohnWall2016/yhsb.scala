import yhsb.base.command.Command

class Payment(args: Seq[String]) extends Command(args) {
  banner("财务支付单生成程序")

  val yearMonth = trailArg[String](
    descr = "发放年月: 格式 YYYYMM, 如 201901"
  )

  override def execute(): Unit = {

  }
}

object Main {
  def main(args: Array[String]) = new Payment(args).runCommand()
}