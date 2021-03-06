package org.sparkle.nlp4j

import edu.emory.mathcs.nlp.common.util.Language
import edu.emory.mathcs.nlp.component.tokenizer.EnglishTokenizer
import org.sparkle.preprocess
import org.sparkle.slate.{Span, _}
import org.sparkle.typesystem.basic.{Sentence, Token}
import org.sparkle.typesystem.ops.sparkle.{SparkleSentenceOps, SparkleTokenOps}
import org.sparkle.typesystem.ops.{SentenceOps, TokenOps, WindowOps}

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._


/**
  * Base wrapper for NLP4J tokenization functionality.
  * @param language
  * @tparam TOKEN
  */
abstract class Nlp4jTokenizerImplBase[TOKEN](language: Language = Language.ENGLISH, windowOps: Option[WindowOps[_]]=None)
    extends StringAnalysisFunction with Serializable {

  require(language == Language.ENGLISH, s"Language $language unsupported in Sparkle NLP4j Tokenizer Wrapper.")

  val tokenOps: TokenOps[TOKEN]

  lazy val tokenizer = new EnglishTokenizer()

  def processText(text: String, textOffset: Int): Seq[(Span, TOKEN)] = {
    val tokensAsNlpNodes = tokenizer.tokenize(text)

    val spansAndTokens =  tokensAsNlpNodes.map { node =>
      val span = Span(node.getStartOffset, node.getEndOffset)
      val token = tokenOps.create(node.getWordForm)
      (span, token)
    }

    spansAndTokens
  }

  override def apply(slate: StringSlate): StringSlate =  {

    if (windowOps.isEmpty) {
      // no window op specified, so run on full text of slate
      val text = slate.content
      val spansAndTokens = this.processText(text, 0)
      tokenOps.addTokens(slate, spansAndTokens)
    } else {
      // Use window op to extract valid spans for tokenizing
      val spansAndTokens = (for ((windowSpan, window) <- windowOps.get.selectWindows(slate)) yield {
        val text = slate.spanned(windowSpan)
        this.processText(text, windowSpan.begin)
      }).flatten
      tokenOps.addTokens(slate, spansAndTokens)
    }
  }
}

abstract class Nlp4jSentenceSegmenterAndTokenizerImplBase[SENTENCE, TOKEN](
  language: Language = Language.ENGLISH, addSentences: Boolean=true, addTokens: Boolean=true, windowOps: Option[WindowOps[_]]=None)
  extends preprocess.SparkleSentenceSegmenterAndTokenizer[SENTENCE, TOKEN] {

  require(language == Language.ENGLISH, s"Language $language unsupported in Sparkle NLP4j Tokenizer Wrapper.")

  val sentenceOps: SentenceOps[SENTENCE]
  val tokenOps: TokenOps[TOKEN]

  lazy val tokenizer = new EnglishTokenizer()


  def processText(text: String, textOffset: Int): (Seq[(Span, SENTENCE)], Seq[(Span, TOKEN)]) = {
    // Convert slate text to an input stream and run with NLP4J
    val sentencesAsNlpNodes = tokenizer.segmentize(text)


    val tokens = new ListBuffer[(Span, TOKEN)]
    val spansAndSentences = sentencesAsNlpNodes.map(
      nlp4jSentence => {
        val span = Span(textOffset + nlp4jSentence(0).getStartOffset, textOffset + nlp4jSentence(nlp4jSentence.length - 1).getEndOffset)
        val sentence = sentenceOps.createSentence()
        (span, sentence)
      }
    )

    val spansAndTokens = sentencesAsNlpNodes.flatMap(_.map(node => {
      val span = Span(textOffset + node.getStartOffset, textOffset + node.getEndOffset)
      val token = tokenOps.create(node.getWordForm)
      (span, token)
    })
    )

    (spansAndSentences, spansAndTokens)
  }

  override def apply(slate: StringSlate): StringSlate = {

    if (windowOps.isEmpty) {
      // No windowOps specified, so process the whole document as one chunk
      // Convert slate text to an input stream and run with NLP4J
      val text = slate.content
      val (spansAndSentences, spansAndTokens) = this.processText(text, 0)
      val slateWithSentences = if (addSentences) sentenceOps.addSentences(slate, spansAndSentences) else slate
      if (addTokens) tokenOps.addTokens(slateWithSentences, spansAndTokens) else slateWithSentences
    } else {
      // run segmenter/tokenizer on windows approved by the window ops
      val sentencesAndTokens = (for ((windowSpan, window) <- windowOps.get.selectWindows(slate)) yield {
        val text = slate.spanned(windowSpan)
        this.processText(text, windowSpan.begin)
      }).toSeq

      // Add annotations to the slate
      var slateOut: StringSlate = slate
      val spansAndSentences = sentencesAndTokens.flatMap(windowSentencesAndTokens => windowSentencesAndTokens._1)
      val spansAndTokens = sentencesAndTokens.flatMap(windowSentencesAndTokens => windowSentencesAndTokens._2)
      if (addSentences) { slateOut = sentenceOps.addSentences(slateOut, spansAndSentences) }
      if (addTokens) { slateOut = tokenOps.addTokens(slateOut, spansAndTokens) }

      slateOut
    }
  }
}



class Nlp4jTokenizerWithSparkleTypes(language: Language=Language.ENGLISH,
                                     windowOps: Option[WindowOps[_]]=None) extends Nlp4jTokenizerImplBase[Token](language) {
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
                                                         addTokens: Boolean=true,
                                                         windowOps: Option[WindowOps[_]]=None)
  extends Nlp4jSentenceSegmenterAndTokenizerImplBase[Sentence, Token](language, addSentences, addTokens, windowOps) {

  override val sentenceOps: SentenceOps[Sentence] = SparkleSentenceOps
  override val tokenOps: TokenOps[Token] = SparkleTokenOps

}

