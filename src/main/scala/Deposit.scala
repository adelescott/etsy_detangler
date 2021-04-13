import java.util.Date
import Utils._

case class Deposit(date: Date, title: String) extends EtsyTransaction {
  val extractDepositAmount: String => Either[String, BigDecimal] = title => {
    val pattern = """^AU\$(\d+,?\d+\.\d+) sent to your bank account$""".r
    title match {
      case pattern(amount) =>
        Right(BigDecimal(amount.replaceAll(",", "").toDouble))
      case _ => Left("Could not extract deposit amount.")
    }
  }

  override def toManagerTransactions(
      etsyOrders: Map[String, EtsyOrder],
      etsySoldItems: Map[String, EtsySoldItem]
  ): Either[String, List[ManagerTransaction]] = {
    val managerTransactions = for {
      depositAmount <- extractDepositAmount(title)
    } yield List(ManagerTransaction(date, "Deposit: " + title, -depositAmount))
    managerTransactions.mapLeft(err =>
      s"Could not process Deposit transaction. $err Deposit transaction: ${this.toString}"
    )
  }
}
