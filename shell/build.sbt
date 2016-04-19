/*
lazy val scalaNlpAndDependencies = Seq(
  //"org.scalanlp" % "epic_2.10" % "0.4-SNAPSHOT",
  //"org.scalanlp" % "epic_2.10" % "0.3.1",
  "org.spire-math" % "spire_2.10" % "0.11.0"
)
libraryDependencies ++= scalaNlpAndDependencies
*/
lazy val sparkAndDependencies = Seq(
  "org.apache.spark" % "spark-core_2.10" % "1.6.1",
  "org.apache.spark" % "spark-sql_2.10" % "1.6.1",
  "org.apache.spark" % "spark-hive_2.10" % "1.6.1",
  "org.apache.spark" % "spark-mllib_2.10" % "1.6.1",
  "org.apache.spark" % "spark-repl_2.10" % "1.6.1"
)
libraryDependencies ++= sparkAndDependencies.map(_ % "provided")

// Add test dependencies
libraryDependencies += "org.scalatest" % "scalatest_2.10" % "2.2.5" % Test
