name := "etsy_detangler"

version := "0.1"

scalaVersion := "2.13.5"
scalacOptions += "-deprecation"

libraryDependencies += "com.nrinaudo" %% "kantan.csv-generic" % "0.6.0"
libraryDependencies += "org.scala-lang.modules" %% "scala-parallel-collections" % "0.2.0"
libraryDependencies += "org.rogach" %% "scallop" % "3.3.1"
