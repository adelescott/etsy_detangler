import scala.collection.parallel.CollectionConverters._
import java.io.File

import kantan.csv._
import kantan.csv.ops._
import Utils._
import kantan.csv.engine.ReaderEngine


/*
Separate Commission into a SaleCommission and ShippingCommission.
Create a CardProcessingFee, this is not part of a Sale or Refund transactions any more.
Update the shipping refund guess to $9.50.
Rename Listing to ListingFee.
A "Fee" supertype now covers: ListingFee, SaleCommission, ShippingCommission, CardProcessingFee. All of these have zero amount, and non-zero fee.
Discard all the fee validation. Just need the EtsyOrders now to separate the postage from the sale amount.
Change Sale validation. Fees should be zero, amount + fees should = net.
*/

object Main extends App {

  /**
    * Reads a csv file defined by the given HeaderDecoder e.g. EstyStatementRow.
    */
  def readCsv[A: HeaderDecoder](filename: String): List[ReadResult[A]] = {
    val file = new File(filename)
    file.asCsvReader[A](rfc.withHeader()).toList
  }

  /**
    * Possibly unused?
    */
  def readHeader[A: HeaderDecoder](
      filename: String
  )(implicit e: ReaderEngine): Either[ParseError, ReadResult[Seq[String]]] = {
    val file = new File(filename)
    val maybeReader = CsvSource[File].open(file)
    maybeReader.map(reader => {
      val data = e.readerFor(reader, rfc.withHeader())
      data.next()
    })
  }

  val cliParser = CliParser(args.toSeq)

  // Read all csv files and parse into their row types
  val etsyStatementRowsReadResults      = readCsv[EtsyStatementRow](cliParser.etsyStatementFilename())
  val etsyOrdersCurrMonthReadResults    = readCsv[EtsyOrder](cliParser.etsyOrdersFilename())
  val etsyOrdersPrevMonthReadResults    = readCsv[EtsyOrder](cliParser.etsyOrdersFilenamePrevMonth())

  // Combine orders from the previous month and this month, and coalese from a list of eithers into an
  // either of list, for convenience.
  val etsyOrdersReadResult = coalesceEithers(etsyOrdersCurrMonthReadResults ++ etsyOrdersPrevMonthReadResults)

  // Process each Etsy statement row, converting it into one or more transactions.
  val transactionsResults = etsyStatementRowsReadResults.par.map {
    etsyStatementRowReadResult =>
      for {
        etsyOrders <- etsyOrdersReadResult
        etsyStatementRow <- etsyStatementRowReadResult
        transactions <- etsyStatementRow.toTransactions(EtsyOrder.indexedByOrderId(etsyOrders))
      } yield transactions
  }

  // Combine all the errors into one.
  val transactionsResult = coalesceEithers(transactionsResults.toList).map(_.flatten)

  // Write the output file, or fail.
  transactionsResult match {
    case Right(transactions) =>
      val outputFile = new File(
        cliParser.outputDir() + "/detangled_etsy_statement.csv"
      )
      try {
        outputFile.writeCsv[Transaction](
          transactions,
          rfc.withHeader("Date", "Reference", "Payee", "Description", "Amount")
        )
      } catch {
        case err: Exception =>
          println(
            s"Error(s) detangling Etsy:\n${err.toString}\nNo output file written."
          )
      }
    case Left(error) =>
      println(s"Error(s) detangling Etsy: $error\nNo output file written.")
  }
}
