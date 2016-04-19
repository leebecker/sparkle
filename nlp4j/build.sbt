lazy val nlp4jAndDependencies = Seq(
  "edu.emory.mathcs.nlp" % "nlp4j" % "1.0.0",
  "edu.emory.mathcs.nlp" % "nlp4j-core" % "1.0.1",
  "edu.emory.mathcs.nlp" % "nlp4j-tokenization" % "1.0.0",
  "edu.emory.mathcs.nlp" % "nlp4j-morphology" % "1.0.0",
  "edu.emory.mathcs.nlp" % "nlp4j-english" % "1.0.0"
)
libraryDependencies ++= nlp4jAndDependencies
