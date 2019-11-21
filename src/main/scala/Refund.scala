import java.util.Date

case class Refund(date: Date, title: String, amount: BigDecimal, feesAndTaxes: BigDecimal) extends EtsyTransaction {
  // It's impossible to know whether the refund was for postage or for a sale. We can somewhat infer it from
  // the price though. $8.50 almost certainly means a postage refund, so we'll put this down as postage refund, else
  // sales refund.
  override def toManagerTransactions(
    etsyOrders: Map[String, EtsyOrder],
    etsySoldItems: Map[String, EtsySoldItem]
  ): Either[String, List[ManagerTransaction]] =
    Right(List(
      ManagerTransaction(date, if (amount == BigDecimal(-8.5))  "Postage refund: " + title else "Sale refund: " + title, amount),
      ManagerTransaction(date, s"Card processing fees refund: $title", feesAndTaxes)
    ))
}
