import java.util.Date

trait EtsyTransaction {
  def date: Date
  def title: String
  def toManagerTransactions(
    etsyOrders: Map[String, EtsyOrder],
    etsySoldItems: Map[String, EtsySoldItem]
  ): Either[String, List[ManagerTransaction]]

  def validateFee(
    fee: BigDecimal,
    total: BigDecimal,
    rate: Double,
    flatFee: BigDecimal,
    feeName: String
  ): Either[String, BigDecimal] = {
    val expectedFee = (-(flatFee + total * rate)).setScale(2, BigDecimal.RoundingMode.HALF_UP)
    if (fee == expectedFee)
      Right(fee)
    else
      Left(feeName + " amount was not as expected. Expected " + expectedFee + " but actual fee was " + fee + ".")
  }
}
