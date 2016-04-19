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

libraryDependencies += "org.apache.commons" % "commons-io" % "1.3.2"

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.10"

lazy val sparkAndDependencies = Seq(
  "org.apache.spark" % "spark-core_2.10" % "1.6.1",
  "org.apache.spark" % "spark-sql_2.10" % "1.6.1",
  "org.apache.spark" % "spark-hive_2.10" % "1.6.1",
  "org.apache.spark" % "spark-mllib_2.10" % "1.6.1",
  "org.apache.spark" % "spark-repl_2.10" % "1.6.1"
)
libraryDependencies ++= sparkAndDependencies.map(_ % "provided")
