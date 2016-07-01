lazy val sparkVersion = "1.6.1"
lazy val sparkAndDependencies = Seq(
  "org.apache.spark" % "spark-core_2.10" % sparkVersion,
  "org.apache.spark" % "spark-sql_2.10" % sparkVersion,
  "org.apache.spark" % "spark-hive_2.10" % sparkVersion,
  "org.apache.spark" % "spark-mllib_2.10" % sparkVersion,
  "org.apache.spark" % "spark-repl_2.10" % sparkVersion
)
libraryDependencies ++= sparkAndDependencies.map(_ % "provided")
