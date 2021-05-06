package yhsb.cjb

import utest._

import yhsb.base.json.Json
import yhsb.cjb.net.protocol.PayState

object CommonTest extends TestSuite {
  def tests = Tests {
    test("paystate") {
      println(
        s"${PayState.Wait}==${PayState.Val("0")}: ${PayState.Wait == PayState.Val("0")}"
      )
      println(
        s"${PayState.Wait}==${PayState.Sucess}: ${PayState.Wait == PayState.Sucess}"
      )
      println(
        Json.toJson(PayState.Wait)
      )
    }
  }
}