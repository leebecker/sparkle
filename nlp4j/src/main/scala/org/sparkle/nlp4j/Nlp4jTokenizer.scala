package org.sparkle.nlp4j

import edu.emory.mathcs.nlp.common.util.Language
import edu.emory.mathcs.nlp.component.tokenizer.EnglishTokenizer
import org.sparkle.preprocess
import org.sparkle.slate.{Span, _}
import org.sparkle.typesystem.basic.{Sentence, Token}
import org.sparkle.typesystem.ops.sparkle.{SparkleSentenceOps, SparkleTokenOps}
import org.sparkle.typesystem.ops.{SentenceOps, TokenOps}

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._


/**
  * Base wrapper for NLP4J tokenization functionality.
  * @param language
  * @tparam TOKEN
  */
abstract class Nlp4jTokenizerImplBase[TOKEN](language: Language = Language.ENGLISH)
    extends StringAnalysisFunction with Serializable {

  require(language == Language.ENGLISH, s"Language $language unsupported in Sparkle NLP4j Tokenizer Wrapper.")

  val tokenOps: TokenOps[TOKEN]

  lazy val tokenizer = new EnglishTokenizer()

  override def apply(slate: StringSlate): StringSlate =  {
    // Convert slate text to an input stream and run with NLP4J
    val text = slate.content
    val tokensAsNlpNodes = tokenizer.tokenize(text)

    val spansAndTokens =  tokensAsNlpNodes.map { node =>
      val span = Span(node.getStartOffset, node.getEndOffset)
      val token = tokenOps.create(node.getWordForm)
      (span, token)
    }

    // Create new annotations and add to a flat list
    tokenOps.addTokens(slate, spansAndTokens)
  }
}



abstract class Nlp4jSentenceSegmenterAndTokenizerImplBase[SENTENCE, TOKEN](
  language: Language = Language.ENGLISH, addSentences: Boolean=true, addTokens: Boolean=true)
  extends preprocess.SparkleSentenceSegmenterAndTokenizer[SENTENCE, TOKEN] {

  require(language == Language.ENGLISH, s"Language $language unsupported in Sparkle NLP4j Tokenizer Wrapper.")

  val sentenceOps: SentenceOps[SENTENCE]
  val tokenOps: TokenOps[TOKEN]

  lazy val tokenizer = new EnglishTokenizer()

  override def apply(slate: StringSlate): StringSlate =  {
    // Convert slate text to an input stream and run with NLP4J
    val text = slate.content
    val sentencesAsNlpNodes = tokenizer.segmentize(text)

    val tokens = new ListBuffer[(Span, TOKEN)]
    val spansAndSentences = sentencesAsNlpNodes.map(
      nlp4jSentence => {
        val span = Span(nlp4jSentence(0).getStartOffset, nlp4jSentence(nlp4jSentence.length - 1).getEndOffset)
        val sentence = sentenceOps.createSentence()
        (span, sentence)
      }
    )

    val spansAndTokens =  sentencesAsNlpNodes.map(_.map(node => {
        val span = Span(node.getStartOffset, node.getEndOffset)
        val token = tokenOps.create(node.getWordForm)
        (span, token)
      })
    ).flatMap(t=>t)

    // Create new Add annotations to a
    val slateWithSentences = if (addSentences) sentenceOps.addSentences(slate, spansAndSentences) else slate
    if (addTokens) tokenOps.addTokens(slateWithSentences, spansAndTokens) else slateWithSentences
  }
}



class Nlp4jTokenizerWithSparkleTypes(language: Language=Language.ENGLISH) extends Nlp4jTokenizerImplBase[Token](language) {
  override val tokenOps: TokenOps[Token] = SparkleTokenOps
}

/**
  * SparkLE wrapper for NLP4J Sentence Segmenter + Tokenizer Combo <p>
  *
  * Prerequisites: StringSlate object <br>
  * Outputs: new StringSlate object with Sentence and Token annotations <br>
  */
class Nlp4jSentenceSegmenterAndTokenizerWithSparkleTypes(language: Language=Language.ENGLISH,
                                                         addSentences: Boolean=true,
                                                         addTokens: Boolean=true)
  extends Nlp4jSentenceSegmenterAndTokenizerImplBase[Sentence, Token](language, addSentences, addTokens) {

  override val sentenceOps: SentenceOps[Sentence] = SparkleSentenceOps
  override val tokenOps: TokenOps[Token] = SparkleTokenOps

}

