import kantan.csv.HeaderDecoder

case class EtsyOrder(
    orderId: String,
    deliveryCountry: String,
    orderValue: BigDecimal,
    deliveryValue: BigDecimal,
    orderTotal: BigDecimal,
    cardProcessingFee: BigDecimal,
    orderNet: BigDecimal
)

object EtsyOrder {
  def indexedByOrderId(etsyOrders: List[EtsyOrder]): Map[String, EtsyOrder] =
    etsyOrders.map(order => order.orderId -> order).toMap

  implicit val etsyOrderDecoder: HeaderDecoder[EtsyOrder] =
    HeaderDecoder.decoder(
      "Order ID",
      "Delivery Country",
      "Order Value",
      "Delivery",
      "Order total",
      "Card Processing Fees",
      "Order Net"
    )(EtsyOrder.apply)
}
