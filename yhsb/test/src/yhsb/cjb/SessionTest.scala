package yhsb.cjb

import utest._
import yhsb.cjb.net.Session
import yhsb.cjb.net.protocol._
import scala.collection.mutable.LinkedHashMap

object SessionTest extends TestSuite {
  def tests = Tests {
    test("login") {
      Session.use(autoLogin = false) { sess =>
        println("=" * 50)
        println(sess.login())
        println("=" * 50)
        println(sess.logout())
      }
    }
    test("cbxx") {
      Session.use() { sess =>
        sess.sendService(PersonInfoInProvinceQuery("430311194511291027"))
        val result = sess.getResult[PersonInfoInProvinceQuery.Item]
        println(result)
        result.foreach { cbxx =>
          println(cbxx)
          println(
            s"${cbxx.cbState} ${cbxx.jfState} ${cbxx.jbKind} ${cbxx.jbState}"
          )
          println(cbxx.czName)
          println(Division.getDwName(cbxx.czName))
        }
      }
    }
    test("cbsh") {
      Session.use() { sess =>
        sess.sendService(JoinAuditQuery("2020-07-20"))
        val result = sess.getResult[JoinAuditQuery.Item]
        result.foreach { cbsh =>
          println(cbsh)
        }
      }
    }
    test("jfcx") {
      Session.use() { sess =>
        sess.sendService(PayingInfoInProvinceQuery("430302197604224525"))
        //sess.sendService(PayingInfoInProvinceQuery("110108196511289010"))
        val result = sess.getResult[PayingInfoInProvinceQuery.Item]
        result.foreach(println(_))
      }
    }
    test("audit") {
      Session.use() { sess =>
        val result = sess.request(JoinAuditQuery())
        println(result)
        sess.sendService(JoinAuditQuery())
        val result2 = sess.getResult[JoinAuditQuery.Item]
        println(result2)
        println(result2(0).dwName)
      }
    }
    test("export") {
      import java.nio.file.Files
      import yhsb.base.util._
      import yhsb.base.excel.Excel._

      val exportFile = Files.createTempFile("yhsb", ".xls").toString
      Session.use() {
        _.exportTo(
          RetiredPersonPauseQuery().set(_.pageSize = null),
          RetiredPersonPauseQuery.ColumnMap
        )(
          exportFile
        )
      }

      val workbook = Excel.load(exportFile)
      val sheet = workbook.getSheetAt(0)
      sheet.setColumnWidth(0, 35 * 256)
      sheet.setColumnWidth(2, 20 * 256)
      sheet.setColumnWidth(3, 8 * 256)

      sheet.deleteRowIf(startRow = 1) {
        _("H").value == "月度拨付触发暂停"
      }

      workbook.save("e:\\retire.xls")
    }
  }
}
