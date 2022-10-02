import org.rogach.scallop.{ScallopConf, ScallopOption}

case class CliParser(arguments: Seq[String]) extends ScallopConf(arguments) {
  val etsyStatementFilename: ScallopOption[String] = opt[String](
    required = true,
    short = 's',
    descr = "Etsy statement filename."
  )
  val etsyOrdersFilename: ScallopOption[String] = opt[String](
    required = true,
    short = 'd',
    descr = "Etsy orders filename."
  )
  val etsyOrdersFilenamePrevMonth: ScallopOption[String] = opt[String](
    required = true,
    short = 'e',
    descr = "Etsy orders filename for previous month."
  )
  val outputDir: ScallopOption[String] = opt[String](
    short = 'o',
    descr = "Directory to write detangled output csv filename to.",
    default = Some(".")
  )
  verify()
}
