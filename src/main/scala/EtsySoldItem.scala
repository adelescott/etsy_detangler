import kantan.csv.HeaderDecoder

case class EtsySoldItem(
    transactionId: String,
    orderId: String,
    itemTotal: BigDecimal
)

object EtsySoldItem {
  def indexedByTransactionId(
      etsySoldItems: List[EtsySoldItem]
  ): Map[String, EtsySoldItem] =
    etsySoldItems.map(order => order.transactionId -> order).toMap

  implicit val etsySoldItemRowDecoder: HeaderDecoder[EtsySoldItem] =
    HeaderDecoder.decoder("Transaction ID", "Order ID", "Item Total")(
      EtsySoldItem.apply
    )
}
