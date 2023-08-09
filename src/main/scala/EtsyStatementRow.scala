import java.text.SimpleDateFormat
import java.util.Date

import kantan.csv.RowDecoder

import Utils._

/** Class to represent a row of an Etsy statement csv. toTransaction performs some basic validation of each row
  * based on my assumptions about how the row should look, and converts it into a stronger typed object based on the
  * Etsy transaction type.
  */
case class EtsyStatementRow(
    date: Date,
    transactionType: String,
    title: String,
    infoOpt: Option[String],
    amount: BigDecimal,
    feesAndTaxes: BigDecimal,
    net: BigDecimal,
    taxDetails: String
) {
  def toTransactions(
      etsyOrders: Map[String, EtsyOrder]
  ): Either[String, List[Transaction]] =
    transactionType match {
      case "Fee"      => toFee
      case "Delivery" => toShippingLabel
      case "Sale"     => toSale(etsyOrders)
      case "Deposit"  => toDeposit
      case "Refund"   => toRefund
      case "Payment"  => toPayment
      case other      => Left("Unknown transaction type " + other)
    }

  /** Validates that net is equal to amount plus fees and taxes. Returns the net.
    */
  def validateNet: Either[String, BigDecimal] =
    if (net != amount + feesAndTaxes)
      Left(
        s"Net was not equal to amount plus fees and taxes."
      )
    else
      Right(net)

  /** Validates that fees and taxes is non-zero and amount is zero. Returns the fees and taxes.
    */
  def validateFeesAndTaxes: Either[String, BigDecimal] =
    if (feesAndTaxes == 0 || amount != 0)
      Left(
        s"Fees and taxes was zero or amount was non-zero."
      )
    else
      Right(feesAndTaxes)

  /** Validates that amount is non-zero and fees and taxes is zero. Returns the amount.
    */
  def validateAmount: Either[String, BigDecimal] =
    if (amount == 0 || feesAndTaxes != 0)
      Left(
        s"Amount was zero or fees and taxes was non-zero."
      )
    else
      Right(amount)

  /** Validates that info was provided. Returns the info.
    */
  def validateInfo: Either[String, String] = infoOpt.toRight(
    s"""No "Info" provided."""
  )

  /** This fee could be one of:
    * * Card processing fee
    * * Card processing fee credit
    * * Sale commission
    * * Sale commission credit
    * * Shipping commission
    * * Shipping commission credit
    * * Listing fee
    * but we leave it up to Manager filters to disambiguate.
    */
  def toFee: Either[String, List[Transaction]] = {
    for {
      net <- validateNet
      _ <- validateFeesAndTaxes
    } yield List(Transaction(date, title, net))
  }.mapLeft(err =>
    s"Could not process fee transaction. $err Fee transaction: ${this.toString}"
  )

  /** Converts this row into a shipping label transaction. This is when you buy a shipping
    * label from Etsy. The description is in the info.
    */
  def toShippingLabel: Either[String, List[Transaction]] = {
    for {
      net <- validateNet
      _ <- validateFeesAndTaxes
      info <- validateInfo
    } yield List(Transaction(date, info, net))
  }.mapLeft(err =>
    s"Could not process shipping label transaction. $err Shipping label transaction: ${this.toString}"
  )

  /** Converts this row into sales and postage revenue as two separate transactions.
    *
    * Etsy combines revenue from both the product and the shipping in the one line, so we need
    * to separate them out using the orders CSV. And helpfully it gives the order ID not in the
    * info like every other transaction type does, it gives it in the text of the title. So
    * we need to regex it out.
    */
  def toSale(
      etsyOrders: Map[String, EtsyOrder]
  ): Either[String, List[Transaction]] = {
    for {
      net <- validateNet
      _ <- validateAmount
      orderId <- {
        val pattern = """^Payment for Order #(\d+)$""".r
        title match {
          case pattern(orderId) => Right(orderId)
          case _                => Left("Could not extract order ID from transaction title.")
        }
      }
      order <- etsyOrders
        .get(orderId)
        .toRight(s"Could not find order ID $orderId in orders csv file.")
      _ <- {
        if (order.orderTotal != net)
          Left("Order total was not equal to net.")
        else if (order.orderTotal != order.orderValue - order.orderDiscount + order.deliveryValue)
          Left(
            s"Order total was not equal to sale revenue plus shipping revenue minus discounts. Order: ${order.toString}"
          )
        else
          Right(order.orderTotal)
      }
    } yield {
      List(
        Transaction(date, s"Sale revenue: order: $orderId", order.orderValue - order.orderDiscount),
        Transaction(date, s"Shipping revenue: order: $orderId", order.deliveryValue)
      )
    }
  }.mapLeft(err =>
    s"Could not process sale transaction. $err Sale transaction: ${this.toString}"
  )

  /** Converts this row into a deposit. A deposit is when Etsy transfers money into your bank
    * account.
    *
    * Etsy's deposit rows for some reason don't have a net amount, so we need to extract it from
    * the text.
    */
  def toDeposit: Either[String, List[Transaction]] = {
    for {
      _ <- validateNet
      depositAmount <- {
        val pattern =
          """^AU\$((?:\d+,)*\d+\.\d+) sent to your bank account$""".r
        title match {
          case pattern(amount) =>
            Right(BigDecimal(amount.replaceAll(",", "").toDouble))
          case _ => Left("Could not extract amount from deposit transaction.")
        }
      }
    } yield {
      List(Transaction(date, "Deposit: " + title, -depositAmount))
    }
  }.mapLeft(err =>
    s"Could not process deposit transaction. $err Deposit transaction: ${this.toString}"
  )

  /** Converts this row into a refund. It's impossible to know whether the refund was for shipping or
    * for a sale. We can somewhat infer it from the price though. $10 almost certainly means a shipping
    * refund, so we'll put this down as shipping refund, else sales refund.
    */
  def toRefund: Either[String, List[Transaction]] = {
    for {
      net <- validateNet
      _ <- validateAmount
    } yield {
      List(
        Transaction(
          date,
          if (amount == BigDecimal(-10)) "Shipping refund: " + title
          else "Sale refund: " + title,
          net
        )
      )
    }
  }.mapLeft(err =>
    s"Could not process refund transaction. $err Refund transaction: ${this.toString}"
  )

  /** Converts this row into a payment Etsy transaction type. A payment is when Etsy draws funds from your
    * bank account to cover a shortfall in your payment account.
    *
    * It seems that sometimes Etsy places the amount of the payment in Amount and sometimes in Fees & Taxes, depending
    * on whether the payment is for a refund (Amount) or for an otherwise outstanding negative balance (Fees & Taxes).
    */
  def toPayment: Either[String, List[Transaction]] = {
    for {
      net <- validateNet
    } yield {
      List(Transaction(date, title, net))
    }
  }.mapLeft(err =>
    s"Could not process payment transaction. $err Payment transaction: ${this.toString}"
  )
}

object EtsyStatementRow {
  implicit val etsyStatementRowDecoder: RowDecoder[EtsyStatementRow] =
    RowDecoder.ordered {
      (
          date: String,
          transactionType: String,
          title: String,
          info: Option[String],
          _: String, /* Currency */
          amount: String,
          feesAndTaxes: String,
          net: String,
          taxDetails: String
      ) =>
        {
          EtsyStatementRow(
            stringToDate(date),
            transactionType,
            escapeQuotes(title),
            info,
            currencyToNumeric(amount),
            currencyToNumeric(feesAndTaxes),
            currencyToNumeric(net),
            taxDetails
          )
        }
    }

  def stringToDate(str: String): Date = {
    val format = new SimpleDateFormat("dd MMMM, yyyy")
    format.parse(str)
  }

  def escapeQuotes(str: String): String = str
    .replaceAll("""&quot;""", "\"")
    .replaceAll("""&#39;""", "\"")

  def currencyToNumeric(str: String): BigDecimal = {
    val cleansed = str
      .replaceAll("""AU\$""", "")
      .replaceAll("""--""", "")
    if (cleansed.isEmpty)
      0.0
    else
      BigDecimal(cleansed.toDouble)
  }
}
