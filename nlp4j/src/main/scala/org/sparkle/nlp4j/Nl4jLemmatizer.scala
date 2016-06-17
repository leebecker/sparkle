package org.sparkle.nlp4j

import edu.emory.mathcs.nlp.common.util.Language
import org.sparkle.slate._
import org.sparkle.typesystem.ops._
import edu.emory.mathcs.nlp.component.morph.english.EnglishMorphAnalyzer
import org.sparkle.typesystem.basic.Token
import org.sparkle.typesystem.ops.sparkle.{SparkleLemmaOps, SparklePartOfSpeechOps, SparkleTokenOps}


/**
  * Created by leebecker on 6/15/16.
  */
abstract class Nlp4jLemmatizerImplBase[TOKEN, POSTAG, LEMMA](language: Language) extends StringAnalysisFunction with Serializable {
  require(language == Language.ENGLISH, s"Language $language unsupported in Sparkle NLP4j Morphological Analyzer Wrapper.")

  val tokenOps: TokenOps[TOKEN]
  val posTagOps: PartOfSpeechOps[TOKEN, POSTAG]
  val lemmaOps: LemmaOps[TOKEN, LEMMA]
  val lemmatizer = new EnglishMorphAnalyzer()

  override def apply(slate: StringSlate): StringSlate = {
    val tokens = tokenOps.selectAllTokens(slate).toIndexedSeq
    val tokenStrings = tokens.map{case(tokenSpan, token) => tokenOps.getText(slate, tokenSpan, token)}

    val lemmas = tokens.map {
      case (tokenSpan, token) => {
        val text = tokenOps.getText(slate, tokenSpan, token)
        val pos = posTagOps.getPos(slate, tokenSpan)
        val posTag = posTagOps.getPosTag(slate, tokenSpan, pos).orNull
        val lemma = lemmatizer.lemmatize(text, posTag)

        (tokenSpan, lemmaOps.createLemma(lemma, token))
      }

    }

    val result = lemmaOps.addLemmas(slate, lemmas)
    result
  }

}

class Nlp4jLemmatizerWithSparkleTypes(language: Language)
  extends Nlp4jLemmatizerImplBase[Token, Token, Token](language) {

  override val tokenOps: TokenOps[Token] = SparkleTokenOps
  override val posTagOps: PartOfSpeechOps[Token, Token] = SparklePartOfSpeechOps
  override val lemmaOps: LemmaOps[Token, Token] = SparkleLemmaOps
}
