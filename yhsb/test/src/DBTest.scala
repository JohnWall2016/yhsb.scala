package yhsb

import utest.TestSuite
import utest.Tests
import utest.test

import util.AutoClose.use

import cjb.db.FullCover._

object DBTest extends TestSuite {
  def tests = Tests {
    test("fullcover2020") {
      import fullcover._
      val result = run(fc2Stxfsj.filter(_.idcard == "430321196607030587"))
      result.foreach(println(_))
    }
  }
}