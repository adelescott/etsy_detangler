import java.util.Date

case class ShippingLabel(
    date: Date,
    title: String,
    info: String,
    net: BigDecimal
) extends EtsyTransaction {
  override def toManagerTransactions(
      etsyOrders: Map[String, EtsyOrder],
      etsySoldItems: Map[String, EtsySoldItem]
  ): Either[String, List[ManagerTransaction]] =
    Right(List(ManagerTransaction(date, info, net)))
}
