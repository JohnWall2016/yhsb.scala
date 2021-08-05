package yhsb.cjb.net.protocol

import java.{util => ju}

class LookBackTable2Cancel(
    items: ju.List[LookBackTable2Audit.Item],
    memo: String
) extends Request[LookBackTable2Cancel.Item]("hcjsShCancel") {
    val rows = items
    val bz = memo
}

object LookBackTable2Cancel {
    def apply(
        item: LookBackTable2Audit.Item,
        memo: String
    ) = new LookBackTable2Cancel(
        ju.List.of(item),
        memo
    )
    case class Item()
}