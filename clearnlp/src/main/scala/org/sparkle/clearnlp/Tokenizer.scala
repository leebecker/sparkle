package org.sparkle.clearnlp

import java.io.InputStream

import edu.emory.clir.clearnlp.component.utils.NLPUtils
import edu.emory.clir.clearnlp.tokenization.AbstractTokenizer
import edu.emory.clir.clearnlp.util.lang.TLanguage
import org.sparkle.slate.Span
import org.sparkle.slate._
import org.apache.commons.io.IOUtils
import org.sparkle.preprocess.{SparkleSentenceSegmenterAndTokenizer, SparkleTokenizer}
import org.sparkle.typesystem.basic.{Sentence, Token}
import org.sparkle.typesystem.ops.{SentenceOps, SparkleSentenceOps, SparkleTokenOps, TokenOps}

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._
import org.sparkle.preprocess
import scala.reflect.ClassTag

/**
  * Created by leebecker on 1/7/16.
  */

/**
  * Wrapper for ClearNLP's Sentence Segmenter.  Under the hood this does redundant
  * tokenization that gets thrown away.  Use this if you have your own tokenization
  * needs downstream.
  */


object Tokenize {
  def segmentSentences(tokenizer: AbstractTokenizer, slate: StringSlate) = {

    // Convert slate text to an input stream and run with ClearNLP
    val stream: InputStream = IOUtils.toInputStream(slate.content)
    val sentencesAsTokens = tokenizer.segmentize(stream)

    // Convert token strings in each sentence into Token span annotations
    val sentenceAsTokenSpans = sentencesAsTokens.map(sentenceTokens => {
      var offset = 0
      slate.content.indexOf()
      val tokens = new ListBuffer[Tuple2[Span, Token]]()
      for (tokenString:String <- sentenceTokens) {
        val tokenBegin = slate.content.indexOf(tokenString, offset)
        val tokenEnd = tokenBegin + tokenString.length
        if (tokenBegin >= 0 && tokenEnd >= 0) {
          tokens += Tuple2(Span(tokenBegin, tokenEnd), Token(slate.content.substring(tokenBegin, tokenEnd)))
        }
        offset = tokenEnd
      }
      tokens.toList
    })

    // Take the head and tail of each sentence to get the sentence offsets
    val sentenceSpans = sentenceAsTokenSpans.map(sentenceTokenSpans =>
      Tuple2(Span(sentenceTokenSpans.head._1.begin, sentenceTokenSpans.last._1.end), Sentence()))

    slate.addLayer[Sentence](sentenceSpans)
  }

  def tokenize[In<:Sentence](tokenizer: AbstractTokenizer, slate: StringSlate) = {

    val tokenSpans = for ((windowSpan, window) <-slate.iterator[Sentence]) yield {
      // Run tokenizer on window
      val windowText = slate.spanned(windowSpan)
      val stream: InputStream = IOUtils.toInputStream(windowText)
      val windowSentenceTokens = tokenizer.segmentize(stream)

      // Now compute token offsets relative to window
      var offset = 0
      val sentenceAsTokenSpans = for (sentenceTokens <- windowSentenceTokens) yield {
        val tokens = new ListBuffer[Tuple2[Span, Token]]()
        for (tokenString <- sentenceTokens) {
          val windowTokenBegin = windowText.indexOf(tokenString, offset)
          val tokenBegin = windowSpan.begin + windowTokenBegin
          val tokenEnd = tokenBegin + tokenString.length
          if (windowTokenBegin >= 0 && tokenString.length >= 0) {
            tokens += Tuple2(Span(tokenBegin, tokenEnd), Token(slate.spanned(Span(tokenBegin, tokenEnd))))
          }
          offset = windowTokenBegin + tokenString.length
        }
        tokens
      }

      // Flatten within the window
      val windowTokenSpans = sentenceAsTokenSpans.flatten
      windowTokenSpans
    }
    val x = tokenSpans.flatten
    val s = slate.addLayer[Token](x)
    s
  }

}



class Tokenizer(languageCode: String=TLanguage.ENGLISH.toString) extends SparkleTokenizer {

  val tokenizer = NLPUtils.getTokenizer(TLanguage.getType(languageCode))

  override def apply(slate: StringSlate): StringSlate = {
    // Convert slate text to an input stream and run with ClearNLP
    val s = Tokenize.tokenize(tokenizer, slate)
    s
  }
}

class SentenceSegmenter(languageCode:String=TLanguage.ENGLISH.toString) extends preprocess.SparkleSentenceSegmenter {
  val tokenizer = NLPUtils.getTokenizer(TLanguage.getType(languageCode))

  override def apply(slate: StringSlate): StringSlate = {
    Tokenize.segmentSentences(tokenizer, slate)
  }
}


abstract class SentenceSegmenterAndTokenizerImplBase[SENTENCE, TOKEN](
  languageCode: String=TLanguage.ENGLISH.toString)
  extends preprocess.SparkleSentenceSegmenterAndTokenizer[SENTENCE, TOKEN] {

  val sentenceOps: SentenceOps[SENTENCE]
  val tokenOps: TokenOps[TOKEN]

  lazy val tokenizer = NLPUtils.getTokenizer(TLanguage.getType(languageCode))

  override def apply(slate: StringSlate): StringSlate =  {
    // Convert slate text to an input stream and run with ClearNLP
    val stream: InputStream = IOUtils.toInputStream(slate.content)
    val sentencesAsTokens = tokenizer.segmentize(stream)

    // Convert token strings in each sentence into Token span annotations
    val sentenceAsTokenSpans = sentencesAsTokens.map(sentenceTokens => {
      var offset = 0
      slate.content.indexOf()
      val tokens = new ListBuffer[(Span, TOKEN)]()
      for (tokenString:String <- sentenceTokens) {
        val tokenBegin = slate.content.indexOf(tokenString, offset)
        val tokenEnd = tokenBegin + tokenString.length
        if (tokenBegin >= 0 && tokenEnd >= 0) {
          tokens += Tuple2(Span(tokenBegin, tokenEnd), tokenOps.create(slate.content.substring(tokenBegin, tokenEnd)))
        }
        offset = tokenEnd
      }
      tokens.toList
    })

    // Take the head and tail of each sentence to get the sentence offsets
    val sentenceSpans = sentenceAsTokenSpans.map(sentenceTokenSpans =>
      Tuple2(Span(sentenceTokenSpans.head._1.begin, sentenceTokenSpans.last._1.end), sentenceOps.createSentence()))


    // Flatten all the tokens into a single list
    val tokenSpans = sentenceAsTokenSpans.flatMap(t=> t)

    // Create new Add annotations to a
    val slateWithSentences = sentenceOps.addSentences(slate, sentenceSpans)
    tokenOps.addTokens(slateWithSentences, tokenSpans)
  }
}

/**
  * SparkLE wrapper for ClearNLP Sentence Segmenter + Tokenizer Combo <p>
  *
  * Prerequisites: StringSlate object <br>
  * Outputs: new StringSlate object with Sentence and Token annotations <br>
  */
class SentenceSegmenterAndTokenizer(languageCode: String=TLanguage.ENGLISH.toString)
  extends SentenceSegmenterAndTokenizerImplBase[Sentence, Token](languageCode) {

  override val sentenceOps: SentenceOps[Sentence] = SparkleSentenceOps
  override val tokenOps: TokenOps[Token] = SparkleTokenOps

}

