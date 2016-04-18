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