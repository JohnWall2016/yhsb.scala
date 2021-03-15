package yhsb

import utest.TestSuite
import utest.Tests
import utest.test

import base.io.AutoClose.use

import cjb.db.FullCover._

object DBTest extends TestSuite {
  def tests = Tests {
    test("fullcover2020") {
      import fullcover._

      println(fc2Stxfsj.quoted.name)

      val result = run(fc2Stxfsj.filter(_.idcard == "430321196607030587"))
      result.foreach(println(_))

      
    }
  }
}