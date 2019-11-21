import org.rogach.scallop.{ScallopConf, ScallopOption}

case class CliParser(arguments: Seq[String]) extends ScallopConf(arguments) {
  val etsyStatementFilename: ScallopOption[String] = opt[String](
    required = true,
    short = 's',
    descr = "Etsy statement filename.")
  val etsyOrdersFilename: ScallopOption[String] = opt[String](
    required = true,
    short = 'd',
    descr = "Etsy orders filename."
  )
  val etsySoldItemsFilename: ScallopOption[String] = opt[String](
    required = true,
    short = 'i',
    descr = "Etsy sold items filename."
  )
  val outputFilename: ScallopOption[String] = opt[String](
    short = 'o',
    descr = "Detangled output csv filename.",
    default = Some("detangled_etsy_statement.csv")
  )
  verify()
}
