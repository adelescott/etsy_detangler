import scala.collection.parallel.CollectionConverters._
import java.io.File

import kantan.csv._
import kantan.csv.ops._
import Utils._

object Main extends App {

  def readCsv[A: HeaderDecoder](filename: String): List[ReadResult[A]] = {
    val file = new File(filename)
    file.asCsvReader[A](rfc.withHeader()).toList
  }

  val cliParser = CliParser(args)

  val etsyStatementRowsReadResults = readCsv[EtsyStatementRow](cliParser.etsyStatementFilename())
  val etsyOrdersReadResults = readCsv[EtsyOrder](cliParser.etsyOrdersFilename())
  val etsySoldItemsReadResults = readCsv[EtsySoldItem](cliParser.etsySoldItemsFilename())

  val etsyOrdersReadResult = coalesceEithers(etsyOrdersReadResults)
  val etsySoldItemsReadResult = coalesceEithers(etsySoldItemsReadResults)

  val managerTransactionsResults = etsyStatementRowsReadResults.par.map { etsyStatementRowReadResult =>
    for {
      etsyOrders <- etsyOrdersReadResult
      etsySoldItems <- etsySoldItemsReadResult
      etsyStatementRow <- etsyStatementRowReadResult
      etsyTransaction <- etsyStatementRow.toEtsyTransaction
      managerTransaction <- etsyTransaction.toManagerTransactions(
        EtsyOrder.indexedByOrderId(etsyOrders),
        EtsySoldItem.indexedByTransactionId(etsySoldItems)
      )
    } yield managerTransaction
  }

  val managerTransactionsResult = coalesceEithers(managerTransactionsResults.toList).map(_.flatten)

  managerTransactionsResult match {
    case Right(managerTransactions) =>
      val outputFile = new File(cliParser.outputDir() + "/detangled_etsy_statement.csv")
      try {
        outputFile.writeCsv[ManagerTransaction](
          managerTransactions,
          rfc.withHeader("Date", "Reference", "Payee", "Description", "Amount")
        )
      } catch {
        case err: Exception => println(s"Error(s) detangling Etsy:\n${err.toString}")
      }
    case Left(error) => println(s"Error(s) detangling Etsy: $error")
  }
}
