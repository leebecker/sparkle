package org.sparkle

import edu.emory.clir.clearnlp.util.lang.TLanguage

/**
  * Created by leebecker on 4/19/16.
  */
package object clearnlp {
  // Tokenization
  def sentenceSegmenterAndTokenizer(languageCode: String=TLanguage.ENGLISH.toString) = new SentenceSegmenterAndTokenizer(languageCode)
  def sentenceSegmenter(languageCode:String=TLanguage.ENGLISH.toString) = new SentenceSegmenter(languageCode)
  def tokenizer(languageCode:String=TLanguage.ENGLISH.toString) = new Tokenizer(languageCode)

  def posTagger(
    languageCode: String=TLanguage.ENGLISH.toString,
    modelPath: String = "general-en-pos.xz",
    paths: Seq[String] = "brown-rcv1.clean.tokenized-CoNLL03.txt-c1000-freq1.txt.xz" :: Nil
  ) = new PosTaggerWithSparkleTypes(languageCode, modelPath, paths)

  def mpAnalyzer() = MpAnalyzer

  def depParser() = DependencyParser

}
