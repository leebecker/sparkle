package org.sparkle

import org.sparkle.nlp4j.Nlp4jTokenizerWithSparkleTypes
import edu.emory.mathcs.nlp.common.util.Language

/**
  * Package methods for quick creation of NLP4J wrappers
  */
package object nlp4j {
  def tokenizer(language: Language = Language.ENGLISH) = new Nlp4jTokenizerWithSparkleTypes(language)

  def sentenceSegmenterAndTokenizer(language: Language=Language.ENGLISH) = new Nlp4jSentenceSegmenterAndTokenizerWithSparkleTypes(language)

  def lemmatizer(language: Language=Language.ENGLISH) = new Nlp4jLemmatizerWithSparkleTypes(language)

  def posTagger(language: Language=Language.ENGLISH, modelPath: String="/edu/emory/mathcs/nlp/models/en-pos.xz") =
    new Nlp4jPosTaggerWithSparkleTypes(language, modelPath)
}
