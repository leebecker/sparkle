lazy val commonSettings = Seq(
  //name := "sparkle-core",
  organization := "com.sparkle",
  version := "0.1-SNAPSHOT",
  scalaVersion := "2.10.6",
  // Require Java 1.8
  initialize := {
    val required = "1.8"
    val current  = sys.props("java.specification.version")
    assert(current == required, s"Unsupported JDK: java.specification.version $current != $required")
  }
)

// Declare projects
lazy val core = project.
  settings(commonSettings: _*)

lazy val typesystem = project.
  settings(commonSettings: _*).
  dependsOn(core)

lazy val clearnlp = project.
  settings(commonSettings: _*)

lazy val opennlp = project.
  settings(commonSettings: _*)


libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.10.6"
  //"org.scala-lang.modules" % "scala-xml_2.11" % "1.0.4"
)

// Setup spark dependencies with scope=provide
lazy val sparkAndDependencies = Seq(
  "org.apache.spark" % "spark-core_2.10" % "1.6.1",
  "org.apache.spark" % "spark-sql_2.10" % "1.6.1",
  "org.apache.spark" % "spark-hive_2.10" % "1.6.1",
  "org.apache.spark" % "spark-mllib_2.10" % "1.6.1",
  "org.apache.spark" % "spark-repl_2.10" % "1.6.1"
)
libraryDependencies ++= sparkAndDependencies.map(_ % "provided")
libraryDependencies += "jline" % "jline" % "2.14.1"


lazy val scalaNlpAndDependencies = Seq(
  "org.scalanlp" % "epic_2.10" % "0.3.1",
  "org.spire-math" % "spire_2.10" % "0.11.0"
)
libraryDependencies ++= scalaNlpAndDependencies

lazy val clearNlpAndDependencies = Seq(
  "edu.emory.clir" % "clearnlp" % "3.2.0",
  "edu.emory.clir" % "clearnlp-dictionary" % "3.2",
  "edu.emory.clir" % "clearnlp-global-lexica" % "3.1",
  "edu.emory.clir" % "clearnlp-general-en-ner-gazetteer" % "3.0",
  "edu.emory.clir" % "clearnlp-general-en-pos" % "3.2",
  "edu.emory.clir" % "clearnlp-general-en-ner" % "3.1",
  "edu.emory.clir" % "clearnlp-general-en-dep" % "3.2",
  "edu.emory.clir" % "clearnlp-general-en-srl" % "3.0"
)
libraryDependencies ++= clearNlpAndDependencies

libraryDependencies += "org.apache.opennlp" % "opennlp-tools" % "1.6.0" exclude ("jwnl", "jwnl")

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "2.2.5" % Test



initialCommands in console := s"""
val sc = new org.apache.spark.SparkContext("local", "shell")
val sqlContext = new org.apache.spark.sql.hive.HiveContext(sc)
import sqlContext.implicits._
import org.apache.spark.sql.functions._
"""

run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run))
