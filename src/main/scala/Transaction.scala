import java.text.SimpleDateFormat
import java.util.Date

import kantan.csv.RowEncoder

case class Transaction (
  date: Date,
  description: String,
  amount: BigDecimal
  // def toManagerStatementRow: Either[String, ManagerStatementRow]
)

// case class Sale() extends Transaction
// case class SaleRefund() extends Transaction
// case class Shipping() extends Transaction
// case class ShippingRefund() extends Transaction
// case class CardProcessingFee() extends Transaction
// case class CardProcessingFeeRefund() extends Transaction
// case class SaleCommission() extends Transaction
// case class SaleCommissionRefund() extends Transaction
// case class ShippingCommission() extends Transaction
// case class ShippingCommissionRefund() extends Transaction
// case class ListingFee() extends Transaction

object Transaction {
  implicit val transactionEncoder: RowEncoder[Transaction] =
    RowEncoder.ordered { transaction: Transaction =>
      (
        dateToString(transaction.date),
        "",
        "Etsy",
        transaction.description,
        transaction.amount
      )
    }

  def dateToString(date: Date): String = {
    val format = new SimpleDateFormat("dd/MM/yyyy")
    format.format(date)
  }
}
