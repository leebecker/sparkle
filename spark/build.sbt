
/*

object SparkConfig {
  lazy val sparkVersion = "2.0.0"
  lazy val deps = Seq(
    "org.apache.spark" %% "spark-core" % sparkVersion,
    "org.apache.spark" %% "spark-sql" % sparkVersion,
    "org.apache.spark" %% "spark-hive" % sparkVersion,
    "org.apache.spark" %% "spark-mllib" % sparkVersion,
    "org.apache.spark" %% "spark-repl" % sparkVersion
  )
}

libraryDependencies ++= SparkConfig.deps.map(_ % "provided")
*/



lazy val sparkVersion = "2.0.0"
lazy val sparkAndDependencies = Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.spark" %% "spark-hive" % sparkVersion,
  "org.apache.spark" %% "spark-mllib" % sparkVersion,
  "org.apache.spark" %% "spark-repl" % sparkVersion
)
libraryDependencies ++= sparkAndDependencies.map(_ % "provided")

