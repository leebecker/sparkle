import sbt._
import Keys._

/**
  * based on https://github.com/harrah/xsbt/wiki/Getting-Started-Multi-Project
  */
object SparkleBuild extends Build {

  lazy val commonSettings = Seq(
    organization := "com.sparkle",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.10.4",
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
  lazy val root = Project(id = "sparkle",
    base = file(".")) aggregate(core, typesystem, util, clearnlp) dependsOn(core, typesystem, util, clearnlp)


  lazy val core = Project(id = "sparkle-core",
    base = file("core")).
    settings(commonSettings)

  lazy val typesystem = Project(id = "sparkle-typesystem", base = file("typesystem")).
    settings(commonSettings).
    dependsOn(core % "test->test;compile->compile")

  lazy val util = Project(id = "sparkle-util", base = file("util")).
    settings(commonSettings).
    dependsOn(core % "test->test;compile->compile").
    dependsOn(typesystem % "test->test;compile->compile")

  lazy val clearnlp = Project(id = "sparkle-clearnlp", base = file("clearnlp")).
    settings(commonSettings).
    dependsOn(core % "test->test;test->compile;compile->compile").
    dependsOn(typesystem % "test->test;test->compile;compile->compile").
    dependsOn(util % "test->test;test->compile;compile->compile")

  lazy val opennlp = Project(id = "sparkle-opennlp", base = file("opennlp")).
    settings(commonSettings).
    dependsOn(core % "test->test;compile->compile").
    dependsOn(typesystem % "test->test;compile->compile").
    dependsOn(util % "test->test;compile->compile")

  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % "2.10.6"
    //"org.scala-lang.modules" % "scala-xml_2.11" % "1.0.4"
  )

  libraryDependencies += "jline" % "jline" % "2.10.5"

  initialCommands in console := s"""
  val sc = new org.apache.spark.SparkContext("local", "shell")
  val sqlContext = new org.apache.spark.sql.hive.HiveContext(sc)
  import sqlContext.implicits._
  import org.apache.spark.sql.functions._
  """

  // Assembly gets provided as well
  run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run))

}