package yhsb.cjb.net.protocol

import yhsb.cjb.net

object SessionOps {
  implicit class PersonStopAuditQuery(session: net.Session) {
    def retiredPersonStopAuditQuery(
        idCard: String = "",
        auditState: String = "",
        operator: String = "",
        startAuditDate: String = "",
        endAuditDate: String = ""
    ) = session.requestOr(
      RetiredPersonStopAuditQuery(
        idCard,
        auditState,
        operator,
        startAuditDate,
        endAuditDate,
        recieveType = "1"
      ),
      RetiredPersonStopAuditQuery(
        idCard,
        auditState,
        operator,
        startAuditDate,
        endAuditDate,
        recieveType = "2"
      )
    )

    def payingPersonStopAuditQuery(
        idCard: String = "",
        auditState: String = "0",
        operator: String = "",
        startAuditDate: String = "",
        endAuditDate: String = ""
    ) = session.requestOr(
      WorkingPersonStopAuditQuery(
        idCard,
        auditState,
        operator,
        startAuditDate,
        endAuditDate,
        recieveType = "1"
      ),
      WorkingPersonStopAuditQuery(
        idCard,
        auditState,
        operator,
        startAuditDate,
        endAuditDate,
        recieveType = "2"
      )
    )
  }

  implicit class CeaseInfo(session: net.Session) {
    object CeaseType extends Enumeration {
      type CeaseType = Value

      val PayingPause = Value("缴费暂停")
      val RetirePause = Value("待遇暂停")
      val PayingStop = Value("缴费终止")
      val RetiredStop = Value("待遇终止")
    }

    import CeaseType._

    case class CeaseInfo(
        ceaseType: CeaseType,
        reason: String,
        yearMonth: String,
        auditState: AuditState,
        memo: String,
        auditDate: Option[String],
        bankName: Option[String] = None
    )
    def getPauseInfoByIdCard(idCard: String): Option[CeaseInfo] = {
      val rpResult = session
        .request(RetiredPersonPauseAuditQuery(idCard, "1"))
      if (rpResult.nonEmpty) {
        val it = rpResult.maxBy(_.auditDate)
        return Some(
          CeaseInfo(
            RetirePause,
            it.reason.toString,
            it.pauseYearMonth.toString,
            it.auditState,
            it.memo,
            Option(it.auditDate)
          )
        )
      } else {
        val ppResult = session
          .request(WorkingPersonPauseAuditQuery(idCard, "1"))
        if (ppResult.nonEmpty) {
          val it = ppResult.maxBy(_.auditDate)
          return Some(
            CeaseInfo(
              PayingPause,
              it.reason.toString,
              it.pauseYearMonth,
              it.auditState,
              it.memo,
              Option(it.auditDate)
            )
          )
        }
      }
      None
    }

    def getStopInfoByIdCard(
        idCard: String,
        additionalInfo: Boolean = false
    ): Option[CeaseInfo] = {
      val rpResult = PersonStopAuditQuery(session)
        .retiredPersonStopAuditQuery(idCard, "1")
      if (rpResult.nonEmpty) {
        val it = rpResult.head
        val bankName =
          if (additionalInfo) {
            session
              .request(RetiredPersonStopAuditDetailQuery(it))
              .headOption match {
              case Some(item) => Option(item.bankType.name)
              case _          => None
            }
          } else {
            None
          }
        return Some(
          CeaseInfo(
            RetirePause,
            it.reason.toString,
            it.stopYearMonth.toString,
            it.auditState,
            it.memo,
            Option(it.auditDate),
            bankName
          )
        )
      } else {
        val ppResult = PersonStopAuditQuery(session)
          .payingPersonStopAuditQuery(idCard, "1")
        if (ppResult.nonEmpty) {
          val it = ppResult.head
          val bankName =
            if (additionalInfo) {
              session
                .request(WorkingPersonStopAuditDetailQuery(it))
                .headOption match {
                case Some(item) => Option(item.bankType.name)
                case _          => None
              }
            } else {
              None
            }
          return Some(
            CeaseInfo(
              PayingStop,
              it.reason.toString,
              it.stopYearMonth.toString,
              it.auditState,
              it.memo,
              Option(it.auditDate),
              bankName
            )
          )
        }
      }
      None
    }
  }
}
