import java.text.SimpleDateFormat
import java.util.Date

import kantan.csv.RowEncoder

case class ManagerTransaction(
    date: Date,
    description: String,
    amount: BigDecimal
)

object ManagerTransaction {
  implicit val managerTransactionEncoder: RowEncoder[ManagerTransaction] =
    RowEncoder.ordered { managerTransaction: ManagerTransaction =>
      (
        dateToString(managerTransaction.date),
        "",
        "Etsy",
        managerTransaction.description,
        managerTransaction.amount
      )
    }

  def dateToString(date: Date): String = {
    val format = new SimpleDateFormat("dd/MM/yyyy")
    format.format(date)
  }
}
