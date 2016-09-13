import sbt._
import Keys._

/**
  * based on https://github.com/harrah/xsbt/wiki/Getting-Started-Multi-Project
  */


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

object SparkleBuild extends Build {



  lazy val commonSettings = Seq(
    organization := "org.sparkle",
    version := "0.2-SNAPSHOT",
    scalaVersion in ThisBuild := "2.11.8",
    autoScalaLibrary := false,
    ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) },
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

  lazy val root = Project(id = "sparkle", base = file(".")).
    aggregate(core, typesystem, util, clearnlp, nlp4j, spark).
    dependsOn(core, typesystem, util, clearnlp, nlp4j, spark).
    settings(
      aggregate in update := false,
      libraryDependencies ++= SparkConfig.deps.map(_ % "provided"),
      // For some reason jline seems to be causing issues
      //libraryDependencies += "org.scala-lang" % "scala-library" % "2.11.8" exclude("jline", "jline"),
        initialCommands in console := """
        val sparkSession = org.apache.spark.sql.SparkSession.builder.
          master("local[*]").
          enableHiveSupport().
          getOrCreate()

        import sparkSession.implicits._
        import org.apache.spark.sql.functions._

        import org.apache.log4j.Logger
        import org.apache.log4j.Level

        Logger.getLogger("org").setLevel(Level.OFF)
        Logger.getLogger("akka").setLevel(Level.OFF)
      """,
      publish := {},
      publishLocal := {}
    )

  lazy val testutil = Project(id ="sparkle-test-util", base = file("testutil")).
    settings(commonSettings)

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

  lazy val spark = Project(id = "sparkle-spark", base = file("spark")).
    settings(commonSettings).
    dependsOn(core).
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

  libraryDependencies ++= Seq(
    //"org.scala-lang" % "scala-reflect" % "2.10.6"
    //"org.scala-lang.modules" % "scala-xml_2.11" % "1.0.4"
  )


  // Assembly gets provided as well
  //run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run))

}
