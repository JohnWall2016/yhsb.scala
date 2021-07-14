package yhsb.cjb

import utest.{TestSuite, Tests, test}
import yhsb.cjb.db.FullCover.{fc2Stxfsj, fullcover}
import yhsb.cjb.db.RawItem
import yhsb.cjb.db.CjbSession
import yhsb.cjb.db.CjbSessionData

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
        import yhsb.cjb.db.AuthData2021._
/*
        println(historyData.quoted.name)

        val result = run(historyData.filter(_.idCard == "430321196409161690"))
        result.foreach(println)
*/
        val item = RawItem(
          None, None, None, None, None,
          idCard = "430321195304240531",
          None, 
          personType = Some("特困人员"),
          None, None
        )
        val result: List[RawItem] = run(rawData.filter { it =>
          it.idCard == lift(item.idCard) &&
          it.personType == lift(item.personType)
        })
        result.foreach(println)
      }

      test("yhsbdb") {
        import yhsb.cjb.db.YhsbDB._

        println(cjbSessionData.quoted.name)

        run(cjbSessionData.insert(CjbSession("000", None, Some("1111111111111111"))))
      
        val result = run(cjbSessionData.filter(_.user == "000"))
        println(result)
      }
    }
}
