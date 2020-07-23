package yhsb.util.commands

import org.rogach.scallop._

trait RowRange { _: ScallopConf =>
  val startRow = trailArg[Int](descr = "开始行")
  val endRow = trailArg[Int](descr = "结束行")
}

trait DateRange { _: ScallopConf =>
  val startDate = trailArg[String](descr = "开始时间, 例如: 20200701")
  val endDate = trailArg[String](descr = "结束时间, 例如: 20200723", required = false)
}

trait Export { _: ScallopConf =>
  val export = opt[Boolean](required = false, descr = "是否导出数据")
}
