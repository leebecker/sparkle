lazy val sparkVersion = "2.0.0"
lazy val sparkAndDependencies = Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.spark" %% "spark-hive" % sparkVersion,
  "org.apache.spark" %% "spark-mllib" % sparkVersion,
  "org.apache.spark" %% "spark-repl" % sparkVersion
)
libraryDependencies ++= sparkAndDependencies.map(_ % "provided")


lazy val dependencies = Seq(
  "de.tudarmstadt.ukp.wikipedia" % "de.tudarmstadt.ukp.wikipedia.api" % "1.1.0",
  "de.tudarmstadt.ukp.wikipedia" % "de.tudarmstadt.ukp.wikipedia.parser" % "1.1.0",
  "com.databricks" %% "spark-xml" % "0.4.0"
)

libraryDependencies ++= dependencies
