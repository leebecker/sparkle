package org.sparkle

import org.sparkle.nlp4j.Nlp4jTokenizerWithSparkleTypes
import edu.emory.mathcs.nlp.common.util.Language
import org.sparkle.typesystem.ops.WindowOps

/**
  * Package methods for quick creation of NLP4J wrappers
  */
package object nlp4j {
  /**
    * Convenience method for creating NLP4J tokenizer wrapper
    * @param language
    * @return
    */
  def tokenizer(language: Language = Language.ENGLISH, windowOps: Option[WindowOps[_]]=None) = new Nlp4jTokenizerWithSparkleTypes(language)

  /**
    * Convenience method for creating NLP4J combined sentence segmenter and tokenizer wrapper
    * @param language
    * @return
    */
  def sentenceSegmenterAndTokenizer(language: Language=Language.ENGLISH,
                                    addSentences:Boolean=true,
                                    addTokens:Boolean=true,
                                    windowOps: Option[WindowOps[_]]=None) = new Nlp4jSentenceSegmenterAndTokenizerWithSparkleTypes(language, addSentences, addTokens, windowOps)


  /**
    * Convenience method for creating NLP4J lemmatizer wrapper
    * @param language
    * @return
    */
  def lemmatizer(language: Language=Language.ENGLISH) = new Nlp4jLemmatizerWithSparkleTypes(language)

  /**
    * Convenience method for creating NLP4J dependency parser wrapper
    * @param language
    * @param modelPath
    * @return
    */
  def posTagger(language: Language=Language.ENGLISH, modelPath: String="/edu/emory/mathcs/nlp/models/en-pos.xz") =
    new Nlp4jPosTaggerWithSparkleTypes(language, modelPath)

  def depParser(language: Language=Language.ENGLISH, modelPath: String="/edu/emory/mathcs/nlp/models/en-dep.xz") =
    new Nlp4jDependencyParserWithSparkleTypes(language, modelPath) {}

  // Useful convenience definition for initializing a pipeline for processing English
  lazy val standardEnglishPipeline = sentenceSegmenterAndTokenizer() andThen posTagger() andThen lemmatizer() andThen depParser()
}
