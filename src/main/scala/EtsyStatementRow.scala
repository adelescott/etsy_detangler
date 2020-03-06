import java.text.SimpleDateFormat
import java.util.Date

import kantan.csv.RowDecoder

/**
 * Class to represent a row of an Etsy statement csv. toEtsyTransaction performs some basic validation of each row
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
  net: BigDecimal
) {
  def toEtsyTransaction: Either[String, EtsyTransaction] = transactionType match {
    case "Listing" => toListing
    case "Postage Label" => toShippingLabel
    case "Transaction" => toCommission
    case "Sale" => toSale
    case "Deposit" => toDeposit
    case "Refund" => toRefund
    case "Payment" => toPayment
    case other => Left("Unknown transaction type " + other)
  }

  /**
   * Converts this row into a simple fee transaction type.  A simple fee has a Date, Description, Info and Net amount.
   * This validates that:
   * * fees and taxes is non-zero,
   * * amount is zero,
   * * net is equal to fees and taxes plus amount, and
   * * the info exists.
   */
  def toSimpleFee(
    feeName: String,
    feeConstructor: (Date, String, String, BigDecimal) => EtsyTransaction
  ): Either[String, EtsyTransaction] = infoOpt match {
    case Some(info) =>
      if (feesAndTaxes == 0 || amount != 0 || net != amount + feesAndTaxes)
        Left(s"Could not create $feeName transaction. Fees and taxes was zero, amount was non-zero or net was " +
          s"not equal to amount plus fees and taxes. EtsyStatementRow: ${this.toString}")
      else
        Right(feeConstructor(date, title, info, net))
    case None =>
      Left(s"""Could not create $feeName transaction. No "Info" provided. EtsyStatementRow: ${this.toString}""")
  }

  def toListing: Either[String, EtsyTransaction] = toSimpleFee("Listing", Listing)
  def toShippingLabel: Either[String, EtsyTransaction] = toSimpleFee("Shipping Label", ShippingLabel)
  def toCommission: Either[String, EtsyTransaction] = toSimpleFee("Commission", Commission)

  /**
   * Converts this row into a Sale Etsy transanction type. This validates that:
   * * amount, fees and taxes and net are all non-zero,
   * * info exists.
   *
   * Note that this does NOT validate that amount + fees and taxes = net as this not true when Etsy has automatically
   * collectd and remitted US state sales tax. We don't attempt to separate all the components of a Sale here (sale
   * revenue, postage revenue, tax collected/remitted, credit card processing fees).
   */
  def toSale: Either[String, Sale] = infoOpt match {
    case Some(info) =>
      if (amount == 0 || feesAndTaxes == 0 || net == 0)
        Left("Could not create Sale transaction. Amount, fees & taxes or net was unexpectedly zero. " +
          s"EtsyStatementRow: ${this.toString}")
      else
        Right(Sale(date, title, info, amount, feesAndTaxes, net))
    case None =>
      Left(s"""Could not create Sale transaction. No "Info" provided. EtsyStatementRow: ${this.toString}""")
  }

  /**
   * Converts this row into a Deposit Etsy transaction type. This validates that:
   * * net is zero.
   *
   * Etsy's deposit rows for some reason don't have a net amount, so we extract it from the text later on in the Deposit
   * transaction type.
   */
  def toDeposit: Either[String, Deposit] = {
    if (net != 0)
      Left(s"Could not create Deposit transaction. Net was unexpectedly zero. EtsyStatementRow: ${this.toString}")
    else
      Right(Deposit(date, title))
  }

  /**
   * Converts this row into a Refund Etsy transaction type. This validates that:
   * * amount is non-zero,
   * * fees and taxes is non-zero,
   * * net is equal to fees and taxes plus amount.
   */
  def toRefund: Either[String, Refund] = {
    if(amount == 0 || feesAndTaxes == 0 || net != amount + feesAndTaxes)
      println("Warning: Refund fees and taxes was zero, amount was zero or net was " +
        s"not equal to amount plus fees and taxes. EtsyStatementRow: ${this.toString}")
    Right(Refund(date, title, amount, feesAndTaxes))
  }

  /**
   * Converts this row into a Payment Etsy transaction type. This validates that:
   * * amount plus fees and taxes is equal to net,
   * * net is non-zero.
   *
   * It seems that sometimes Etsy places the amount of the payment in Amount and sometimes in Fees & Taxes, depending
   * on whether the payment is for a refund (Amount) or for an outstanding negative balance (Fees & Taxes).
   */
  def toPayment: Either[String, Payment] =
    if (feesAndTaxes + amount != net || net == 0)
      Left("Could not create Payment transaction. Amount plus fees & taxes wasn't " +
        s"equal to net, or net was zero. EtsyStatementRow: ${this.toString}")
    else
      Right(Payment(date, title, net))
}

object EtsyStatementRow {
  implicit val etsyStatementRowDecoder: RowDecoder[EtsyStatementRow] = RowDecoder.ordered { (
    date: String,
    transactionType: String,
    title: String,
    info: Option[String],
    _: String, /* Currency */
    amount: String,
    feesAndTaxes: String,
    net: String
  ) => {
    EtsyStatementRow(
      stringToDate(date),
      transactionType,
      escapeQuotes(title),
      info,
      currencyToNumeric(amount),
      currencyToNumeric(feesAndTaxes),
      currencyToNumeric(net)
    )
  }}

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