package yhsb.util.commands

import org.rogach.scallop._

trait RowRange { _: ScallopConf =>
  val startRow = trailArg[Int](descr = "开始行, 从1开始")
  val endRow = trailArg[Int](descr = "结束行, 包含在内")
}

trait DateRange { _: ScallopConf =>
  val startDate = trailArg[String](descr = "开始时间, 例如: 20200701")
  val endDate = trailArg[String](descr = "结束时间, 例如: 20200723", required = false)
}

trait Export { _: ScallopConf =>
  val export = opt[Boolean](required = false, descr = "是否导出数据")
}

trait InputFile { _: ScallopConf =>
  val inputFile = trailArg[String](descr = "输入文件路径")
}

trait SheetIndex { _: ScallopConf =>
  val sheetIndex = trailArg[Int](descr = "数据表序号, 默认为0", default = Some(0), required = false)
}
