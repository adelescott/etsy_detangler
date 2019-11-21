import kantan.csv.RowDecoder

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

  implicit val etsyOrderDecoder: RowDecoder[EtsyOrder] =
    RowDecoder.decoder(1, 14, 16, 21, 23, 25,26 )(EtsyOrder.apply)
}

// "Sale Date",
// "Order ID",
// "Buyer User ID",
// "Full Name",
// "First Name",
// "Last Name",
// "Number of Items",
// "Payment Method",
// "Date Posted",
// "Street 1",
// "Street 2",
// "Delivery City",
// "Delivery State",
// "Delivery Zipcode",
// "Delivery Country",
// Currency,
// "Order Value",
// "Coupon Code",
// "Coupon Details",
// "Discount Amount",
// "Delivery Discount",
// Delivery,
// "Sales tax",
// "Order Total",
// Status,
// "Card Processing Fees",
// "Order Net",
// "Adjusted Order Total",
// "Adjusted Card Processing Fees",
// "Adjusted Net Order Amount",
// Buyer,
// "Order Type",
// "Payment Type",
// "InPerson Discount",
// "InPerson Location"