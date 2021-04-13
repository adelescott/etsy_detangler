import java.util.Date
import Utils._

/** Need the etsy orders to separate the sale from the postage.
  */
case class Sale(
    date: Date,
    title: String,
    into: String,
    amount: BigDecimal,
    feesAndTaxes: BigDecimal,
    net: BigDecimal
) extends EtsyTransaction {

  val extractOrderId: String => Either[String, String] = title => {
    val pattern = """^Payment for Order (\d+)$""".r
    title match {
      case pattern(orderId) => Right(orderId)
      case _                => Left("Could not extract order ID from transaction title.")
    }
  }

  /** Called "Etsy Payments payment processing fee" in Etsy speak.
    */
  def validateCardProcessingFee(
      cardProcessingFee: BigDecimal,
      saleRevenue: BigDecimal,
      shippingRevenue: BigDecimal,
      taxCollected: BigDecimal,
      isDomestic: Boolean
  ): Either[String, BigDecimal] = {
    val cardProcessingRate = if (isDomestic) 0.03 else 0.04
    val cardProcessingFlatFee = 0.25
    validateFee(
      cardProcessingFee,
      saleRevenue + shippingRevenue + taxCollected,
      cardProcessingRate,
      cardProcessingFlatFee,
      "Card processing fee"
    )
  }

  /** US state sales tax is automatically collected from customers, and should be automatically deducted from the
    * "net" amount in the Etsy statement.
    */
  def validateTaxCollected(
      saleRevenue: BigDecimal,
      shippingRevenue: BigDecimal,
      orderTotal: BigDecimal,
      amount: BigDecimal,
      feesAndTaxes: BigDecimal,
      net: BigDecimal
  ): Either[String, BigDecimal] = {
    val taxCollected = orderTotal - (saleRevenue + shippingRevenue)
    val taxRemitted = (amount + feesAndTaxes) - net
    if (taxCollected == taxRemitted)
      Right(taxCollected)
    else
      Left(
        "Could not validate the tax collected. Appears as though the tax collected is not equal to the tax " +
          s"remitted. taxCollected = $taxCollected taxRemitted = $taxRemitted Or, some other portion " +
          "of the sale has gone missing."
      )
  }

  override def toManagerTransactions(
      etsyOrders: Map[String, EtsyOrder],
      etsySoldItems: Map[String, EtsySoldItem]
  ): Either[String, List[ManagerTransaction]] = {
    val managerTransactions = for {
      orderId <- extractOrderId(title)
      order <- etsyOrders
        .get(orderId)
        .toRight(
          "No corresponding order found for this sale transaction in orders csv file."
        )
      saleRevenue = order.orderValue
      shippingRevenue = order.deliveryValue
      orderTotal = order.orderTotal
      taxCollected <- validateTaxCollected(
        saleRevenue,
        shippingRevenue,
        orderTotal,
        amount,
        feesAndTaxes,
        net
      )
      isDomestic = order.deliveryCountry == "Australia"
      cardProcessingFee <- validateCardProcessingFee(
        -order.cardProcessingFee,
        saleRevenue,
        shippingRevenue,
        taxCollected,
        isDomestic
      )
      orderRevenueDescription = s"Sale revenue: order: $orderId"
      shippingRevenueDescription = s"Shipping revenue: order: $orderId"
      cardProcessingFeeDescription = s"Card processing fees: order: $orderId"
    } yield List(
      ManagerTransaction(date, orderRevenueDescription, saleRevenue),
      ManagerTransaction(date, shippingRevenueDescription, shippingRevenue),
      ManagerTransaction(date, cardProcessingFeeDescription, cardProcessingFee)
    )
    managerTransactions.mapLeft(err =>
      s"Could not process Sale transaction. $err Sale transaction: ${this.toString}"
    )
  }
}
