import kantan.csv.RowDecoder

case class EtsySoldItem(
  transactionId: String,
  orderId: String,
  itemTotal: BigDecimal
)

object EtsySoldItem {
  def indexedByTransactionId(etsySoldItems: List[EtsySoldItem]): Map[String, EtsySoldItem] =
    etsySoldItems.map(order => order.transactionId -> order).toMap

  implicit val etsySoldItemRowDecoder: RowDecoder[EtsySoldItem] =
    RowDecoder.decoder(13, 24, 11)(EtsySoldItem.apply)
}

// "Sale Date",
// "Item Name",
// Buyer,
// Quantity,
// Price,
// "Coupon Code",
// "Coupon Details",
// "Discount Amount",
// "Delivery Discount",
// "Order Delivery",
// "Order Sales Tax",
// "Item Total",
// Currency,
// "Transaction ID",
// "Listing ID",
// "Date Paid",
// "Date Posted",
// "Delivery Name",
// "Delivery Address1",
// "Delivery Address2",
// "Delivery City",
// "Delivery State",
// "Delivery Zipcode",
// "Delivery Country",
// "Order ID",
// Variations,
// "Order Type",
// "Listings Type",
// "Payment Type",
// "InPerson Discount",
// "InPerson Location",
// "VAT Paid by Buyer"