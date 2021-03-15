package yhsb

import utest.{TestSuite, Tests, test}
import yhsb.cjb.db.FullCover.{fc2Stxfsj, fullcover}

object DBTest extends TestSuite {
  def tests =
    Tests {
      test("fullcover2020") {
        import fullcover._

        println(fc2Stxfsj.quoted.name)

        val result = run(fc2Stxfsj.filter(_.idcard == "430321196607030587"))
        result.foreach(println(_))
      }

      test("fpdata2021") {
        import cjb.db.FPData2021._

        println(fphistoryData.quoted.name)

        val result = run(fphistoryData.filter(_.idcard == "430321196409161690"))
        result.foreach(println)
      }
    }
}
