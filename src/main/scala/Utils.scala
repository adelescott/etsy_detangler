object Utils {
  /**
    * Converts a list of eithers into an either of list.
    */
  def coalesceEithers[A, B](
      eithers: List[Either[A, B]]
  ): Either[String, List[B]] =
    if (eithers.forall(_.isRight))
      Right(eithers.foldLeft(List[B]()) { case (acc, either) =>
        acc ++ either.fold(_ => Nil, List(_))
      })
    else
      Left(eithers.foldLeft("") { case (acc, either) =>
        acc + either.fold(err => "\n" + err.toString, _ => "")
      })

  implicit class eitherImplicits[A, B](either: Either[A, B]) {
    def mapLeft[C](f: A => C): Either[C, B] = either.swap.map(f).swap
  }
}
