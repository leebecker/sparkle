lazy val sparkAndDependencies = Seq(
  "org.apache.spark" % "spark-core_2.10" % "1.6.1",
  "org.apache.spark" % "spark-sql_2.10" % "1.6.1",
  "org.apache.spark" % "spark-hive_2.10" % "1.6.1",
  "org.apache.spark" % "spark-mllib_2.10" % "1.6.1",
  "org.apache.spark" % "spark-repl_2.10" % "1.6.1"
)
libraryDependencies ++= sparkAndDependencies.map(_ % "provided")


// Add test dependencies
libraryDependencies += "org.scalatest" % "scalatest_2.10" % "2.2.5"

//libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.10" % "provided"
//libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.7.10" % "provided"
