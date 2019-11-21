import java.util.Date
import Utils._

/**
 * Need the etsy sold items to validate the sales commissions, and the etsy orders to validate the postage commission.
 */
case class Commission(
  date: Date,
  title: String,
  info: String,
  net: BigDecimal,
) extends EtsyTransaction {
  /**
   * "transaction" in transaction ID refers to each sale
   */
  val extractTransactionId: String => Either[String, String] = info => {
    val pattern = """^transaction: (\d+)$""".r
    info match {
      case pattern(transactionId) => Right(transactionId)
      case _ => Left("Could not extract transaction ID from transaction info.")
    }
  }

  val extractOrderId: String => Either[String, String] = info => {
    val pattern = """^order: (\d+)$""".r
    info match {
      case pattern(orderId) => Right(orderId)
      case _ => Left("Could not extract order ID from transaction info.")
    }
  }

  /**
   * Called "transaction fee" in Etsy speak.
   */
  def validateCommission(
    commission: BigDecimal,
    revenue: BigDecimal
  ): Either[String, BigDecimal] = {
    val commissionRate = 0.05
    val commissionFlatFee = 0.00
    validateFee(
      commission,
      revenue,
      commissionRate,
      commissionFlatFee,
      "Commission"
    )
  }

  override def toManagerTransactions(
    etsyOrders: Map[String, EtsyOrder],
    etsySoldItems: Map[String, EtsySoldItem]
  ): Either[String, List[ManagerTransaction]] = {
    if (net > 0) { // This is a refund
      val descriptionPrefix = title match {
        case "Delivery" => "Shipping commission refund: "
        case _ => "Sale commission refund: "
      }
      Right(List(ManagerTransaction(date, descriptionPrefix + info, net)))
    }
    else {
      title match {
        case "Delivery" =>
          val descriptionPrefix = "Shipping commission: "
          val managerTransactions = for {
            orderId <- extractOrderId(info)
            order <- etsyOrders
              .get(orderId)
              .toRight("No corresponding order found for this commission transaction in orders csv file.")
            shippingRevenue = order.deliveryValue
            commissionFee <- validateCommission(net, shippingRevenue)
          } yield List(ManagerTransaction(date, descriptionPrefix + info, commissionFee))
          managerTransactions.mapLeft(
            err => s"Could not process Commission transaction. $err Commission transaction: ${this.toString}"
          )
        case _ =>
          val descriptionPrefix = "Sale commission: "
          val managerTransactions = for {
            transactionId <- extractTransactionId(info)
            item <- etsySoldItems
              .get(transactionId)
              .toRight("No corresponding sold item found for this commission transaction in sold items csv file.")
            saleRevenue = item.itemTotal
            orderId = item.orderId
            commissionFee <- validateCommission(net, saleRevenue)
          } yield List(ManagerTransaction(date, descriptionPrefix + info + " order: " + orderId, commissionFee))
          managerTransactions.mapLeft(
            err => s"Could not process Commission transaction. $err Commission transaction: ${this.toString}"
          )
      }
    }
  }
}
