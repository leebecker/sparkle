package org.sparkle.nlp4j

import edu.emory.mathcs.nlp.common.util.Language
import org.sparkle.slate._
import org.sparkle.typesystem.ops._
import edu.emory.mathcs.nlp.component.morph.english.EnglishMorphAnalyzer
import org.sparkle.typesystem.basic.Token
import org.sparkle.typesystem.ops.sparkle.{SparkleLemmaOps, SparklePartOfSpeechOps, SparkleTokenOps}


/**
  * Base class for wrapping NLP4J's Morphological analyzer.  Extend this by overriding  the *Ops fields with
  * concrete implementations for a particular typesystem
  *
  * @param language language code for defining models
  * @tparam TOKEN type of annotation containing token information
  * @tparam POSTAG type of annotation containing POS information
  * @tparam LEMMA type of annotation containing lemma information
  */
abstract class Nlp4jLemmatizerImplBase[TOKEN, POSTAG, LEMMA](language: Language, windowOps: Option[WindowOps[_]]=None) extends StringAnalysisFunction with Serializable {
  require(language == Language.ENGLISH, s"Language $language unsupported in Sparkle NLP4j Morphological Analyzer Wrapper.")

  val tokenOps: TokenOps[TOKEN]
  val posTagOps: PartOfSpeechOps[TOKEN, POSTAG]
  val lemmaOps: LemmaOps[TOKEN, LEMMA]

  // initialize NLP4J morpho-analyzer
  // FIXME: Define way to get different models
  lazy val lemmatizer = new EnglishMorphAnalyzer()



  override def apply(slate: StringSlate): StringSlate = {
    val tokens = if (windowOps.isEmpty) {
      // No window ops, get all tokens
      tokenOps.selectAllTokens(slate).toIndexedSeq
    } else {
      // Use window ops to select relevant tokens
      tokenOps.selectAllTokens(slate).toIndexedSeq
      windowOps.get.selectWindows(slate).map{ case(windowSpan, window) => tokenOps.selectTokens(slate, windowSpan) }.flatten.toIndexedSeq
    }
    val tokenStrings = tokens.map{case(tokenSpan, token) => tokenOps.getText(slate, tokenSpan, token)}

    // Extract token and POS information
    val lemmas = tokens.map {
      case (tokenSpan, token) =>
        val text = tokenOps.getText(slate, tokenSpan, token)
        val pos = posTagOps.getPos(slate, tokenSpan)
        val posTag = posTagOps.getPosTag(slate, tokenSpan, pos).orNull
        val lemma = lemmatizer.lemmatize(text, posTag)

        (tokenSpan, lemmaOps.createLemma(lemma, token))
    }

    val result = lemmaOps.addLemmas(slate, lemmas)
    result
  }
}

/**
  * Define Lemmatizer for Sparkle TypeSystem
  * @param language language code for defining models
  */
class Nlp4jLemmatizerWithSparkleTypes(language: Language, windowOps: Option[WindowOps[_]]=None)
  extends Nlp4jLemmatizerImplBase[Token, Token, Token](language) {

  override val tokenOps: TokenOps[Token] = SparkleTokenOps
  override val posTagOps: PartOfSpeechOps[Token, Token] = SparklePartOfSpeechOps
  override val lemmaOps: LemmaOps[Token, Token] = SparkleLemmaOps
}
