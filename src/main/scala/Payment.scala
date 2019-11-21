import java.util.Date

case class Payment(date: Date, title: String, net: BigDecimal) extends EtsyTransaction {
  override def toManagerTransactions(
    etsyOrders: Map[String, EtsyOrder],
    etsySoldItems: Map[String, EtsySoldItem]
  ): Either[String, List[ManagerTransaction]] =
    Right(List(ManagerTransaction(date, s"Charge: $title", net)))
}
