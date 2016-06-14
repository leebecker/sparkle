package org.sparkle

import org.sparkle.nlp4j.Nlp4jTokenizerWithSparkleTypes
import edu.emory.mathcs.nlp.common.util.Language

/**
  * Created by leebecker on 4/19/16.
  */
package object nlp4j {
  def tokenizer(language: Language = Language.ENGLISH) = new Nlp4jTokenizerWithSparkleTypes(language)

  def sentenceSegmenterAndTokenizer(language: Language=Language.ENGLISH) = new Nlp4jSentenceSegmenterAndTokenizerWithSparkleTypes(language)

  def posTagger(language: Language=Language.ENGLISH, modelPath: String="/edu/emory/mathcs/nlp/models/en-pos.xz") =
    new PosTaggerWithSparkleTypes(language, modelPath)
}
