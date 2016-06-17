lazy val nlp4jAndDependencies = Seq(
  "edu.emory.mathcs.nlp" % "nlp4j" % "1.1.1",
  "edu.emory.mathcs.nlp" % "nlp4j-core" % "1.1.1",
  "edu.emory.mathcs.nlp" % "nlp4j-common" % "1.1.1",
  "edu.emory.mathcs.nlp" % "nlp4j-tokenization" % "1.1.1",
  "edu.emory.mathcs.nlp" % "nlp4j-morphology" % "1.1.1",
  "edu.emory.mathcs.nlp" % "nlp4j-english" % "1.1.1"
)
libraryDependencies ++= nlp4jAndDependencies
