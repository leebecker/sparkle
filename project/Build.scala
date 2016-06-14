import sbt._
import Keys._

/**
  * based on https://github.com/harrah/xsbt/wiki/Getting-Started-Multi-Project
  */
object SparkleBuild extends Build {

  lazy val commonSettings = Seq(
    organization := "com.sparkle",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.10.6",
    //ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) },
    // Require Java 1.8
    initialize := {
      val required = "1.8"
      val current  = sys.props("java.specification.version")
      assert(current == required, s"Unsupported JDK: java.specification.version $current != $required")
    }
  )

  // aggregate: running a task on the aggregate project will also run it on the aggregated projects.
  // dependsOn: a project depends on code in another project.
  // without dependsOn, you'll get a compiler error: "object bar is not a member of package
  // com.alvinalexander".
  lazy val sparkAndDependencies2 = Seq(
    "org.apache.spark" % "spark-core_2.10" % "1.6.1",
    "org.apache.spark" % "spark-sql_2.10" % "1.6.1",
    "org.apache.spark" % "spark-hive_2.10" % "1.6.1",
    "org.apache.spark" % "spark-mllib_2.10" % "1.6.1",
    "org.apache.spark" % "spark-repl_2.10" % "1.6.1"
  )

  lazy val root = Project(id = "sparkle", base = file(".")).
    aggregate(core, typesystem, util, clearnlp).
    dependsOn(core, typesystem, util, clearnlp).
    settings(
      libraryDependencies ++= sparkAndDependencies2.map(_ % "provided"),
      initialCommands in console := """
        val sc = new org.apache.spark.SparkContext("local", "shell")
        val sqlContext = new org.apache.spark.sql.hive.HiveContext(sc)
        import sqlContext.implicits._
        import org.apache.spark.sql.functions._
      """
    )

  lazy val testutil = Project(id ="sparkle-test-util", base = file("testutil"))

  lazy val core = Project(id = "sparkle-core",
    base = file("core")).
    settings(commonSettings).
    dependsOn(testutil % "test->compile")

  lazy val typesystem = Project(id = "sparkle-typesystem", base = file("typesystem")).
    settings(commonSettings).
    dependsOn(core).
    dependsOn(testutil % "test->compile")

  lazy val util = Project(id = "sparkle-util", base = file("util")).
    settings(commonSettings).
    dependsOn(core).
    dependsOn(typesystem).
    dependsOn(testutil % "test->compile")

  lazy val clearnlp = Project(id = "sparkle-clearnlp", base = file("clearnlp")).
    settings(commonSettings).
    dependsOn(core).
    dependsOn(typesystem).
    dependsOn(util).
    dependsOn(testutil % "test->compile")

  lazy val nlp4j = Project(id = "sparkle-nlp4j", base = file("nlp4j")).
    settings(commonSettings).
    dependsOn(core).
    dependsOn(typesystem).
    dependsOn(util).
    dependsOn(testutil % "test->compile")

  lazy val opennlp = Project(id = "sparkle-opennlp", base = file("opennlp")).
    settings(commonSettings).
    dependsOn(core % "test->test;compile->compile").
    dependsOn(typesystem % "test->test;compile->compile").
    dependsOn(util % "test->test;compile->compile")

  lazy val shell = Project(id = "sparkle-shell", base = file("shell")).
    settings(commonSettings).
    dependsOn(root).
    settings(
      initialCommands in console := """
        val sc = new org.apache.spark.SparkContext("local", "shell")
        val sqlContext = new org.apache.spark.sql.hive.HiveContext(sc)
        import sqlContext.implicits._
        import org.apache.spark.sql.functions._
      """
    ).
    settings(
      run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run))
    )

  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % "2.10.6"
    //"org.scala-lang.modules" % "scala-xml_2.11" % "1.0.4"
  )

  /*
  libraryDependencies += "jline" % "jline" % "2.10.5"

  lazy val sparkAndDependencies2 = Seq(
    "org.apache.spark" % "spark-core_2.10" % "1.6.1",
    "org.apache.spark" % "spark-sql_2.10" % "1.6.1",
    "org.apache.spark" % "spark-hive_2.10" % "1.6.1",
    "org.apache.spark" % "spark-mllib_2.10" % "1.6.1",
    "org.apache.spark" % "spark-repl_2.10" % "1.6.1"
  )
  libraryDependencies ++= sparkAndDependencies2.map(_ % "provided")

  initialCommands in console := s"""
  val sc = new org.apache.spark.SparkContext("local", "shell")
  val sqlContext = new org.apache.spark.sql.hive.HiveContext(sc)
  import sqlContext.implicits._
  import org.apache.spark.sql.functions._
  """

  // Assembly gets provided as well
  run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run))
  */

}