package org.sparkle.clearnlp

import java.io.InputStream

import edu.emory.clir.clearnlp.component.utils.NLPUtils
import edu.emory.clir.clearnlp.tokenization.AbstractTokenizer
import edu.emory.clir.clearnlp.util.lang.TLanguage
import epic.trees.Span

import epic.slab._
import org.apache.commons.io.IOUtils
import org.sparkle.preprocess.{SparkleTokenizer, SparkleSentenceSegmenterAndTokenizer}
import org.sparkle.typesystem.basic.Token

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._
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
  def segmentSentences[In](tokenizer: AbstractTokenizer, slab: StringSlab[In]) = {

    // Convert slab text to an input stream and run with ClearNLP
    val stream: InputStream = IOUtils.toInputStream(slab.content)
    val sentencesAsTokens = tokenizer.segmentize(stream)

    // Convert token strings in each sentence into Token span annotations
    val sentenceAsTokenSpans = sentencesAsTokens.map(sentenceTokens => {
      var offset = 0
      slab.content.indexOf()
      val tokens = new ListBuffer[Tuple2[Span, Token]]()
      for (tokenString:String <- sentenceTokens) {
        val tokenBegin = slab.content.indexOf(tokenString, offset)
        val tokenEnd = tokenBegin + tokenString.length
        if (tokenBegin >= 0 && tokenEnd >= 0) {
          tokens += Tuple2(Span(tokenBegin, tokenEnd), Token(slab.content.substring(tokenBegin, tokenEnd)))
        }
        offset = tokenEnd
      }
      tokens.toList
    })

    // Take the head and tail of each sentence to get the sentence offsets
    val sentenceSpans = sentenceAsTokenSpans.map(sentenceTokenSpans =>
      Tuple2(Span(sentenceTokenSpans.head._1.begin, sentenceTokenSpans.last._1.end), Sentence()))

    slab.addLayer[Sentence](sentenceSpans)
  }

  def tokenize[In<:Sentence](tokenizer: AbstractTokenizer, slab: StringSlab[In]) = {

    val tokenSpans = for ((windowSpan, window) <-slab.iterator[Sentence]) yield {
      // Run tokenizer on window
      val windowText = slab.spanned(windowSpan)
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
            tokens += Tuple2(Span(tokenBegin, tokenEnd), Token(slab.spanned(Span(tokenBegin, tokenEnd))))
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
    val s = slab.addLayer[Token](x)
    s
  }

}



class Tokenizer(languageCode: String=TLanguage.ENGLISH.toString) extends SparkleTokenizer {

  val tokenizer = NLPUtils.getTokenizer(TLanguage.getType(languageCode))

  override def apply[In<:Sentence](slab: StringSlab[In]): StringSlab[In with Token] = {
    // Convert slab text to an input stream and run with ClearNLP
    val s = Tokenize.tokenize(tokenizer, slab)
    s
  }
}

class SentenceSegmenter(languageCode:String=TLanguage.ENGLISH.toString) extends epic.preprocess.SentenceSegmenter {
  val tokenizer = NLPUtils.getTokenizer(TLanguage.getType(languageCode))

  override def apply[In](slab: epic.slab.StringSlab[In]): StringSlab[In with Sentence] = {
    Tokenize.segmentSentences[In](tokenizer, slab)
  }
}

object ClearNlpTokenization {

  def sentenceSegmenter(languageCode:String=TLanguage.ENGLISH.toString) = new SentenceSegmenter(languageCode)
  def tokenizer(languageCode:String=TLanguage.ENGLISH.toString) = new Tokenizer(languageCode)
}



/**
  * SparkLE wrapper for ClearNLP Sentence Segmenter + Tokenizer Combo <p>
  *
  * Prerequisites: StringSlab object <br>
  * Outputs: new StringSlab object with Sentence and Token annotations <br>
  */
object SentenceSegmenterAndTokenizer extends SparkleSentenceSegmenterAndTokenizer {
  // FIXME parameterize language code and pre-load tokenizer
  val defaultLanguageCode = TLanguage.ENGLISH.toString
  val tokenizer = NLPUtils.getTokenizer(TLanguage.getType(defaultLanguageCode))

  override def apply[In](slab: StringSlab[In]): StringSlab[In with Sentence with Token] =  {

    // Convert slab text to an input stream and run with ClearNLP
    val stream: InputStream = IOUtils.toInputStream(slab.content)
    val sentencesAsTokens = tokenizer.segmentize(stream)

    // Convert token strings in each sentence into Token span annotations
    val sentenceAsTokenSpans = sentencesAsTokens.map(sentenceTokens => {
      var offset = 0
      slab.content.indexOf()
      val tokens = new ListBuffer[Tuple2[Span, Token]]()
      for (tokenString:String <- sentenceTokens) {
        val tokenBegin = slab.content.indexOf(tokenString, offset)
        val tokenEnd = tokenBegin + tokenString.length
        if (tokenBegin >= 0 && tokenEnd >= 0) {
          tokens += Tuple2(Span(tokenBegin, tokenEnd), Token(slab.content.substring(tokenBegin, tokenEnd)))
        }
        offset = tokenEnd
      }
      tokens.toList
    })

    // Take the head and tail of each sentence to get the sentence offsets
    val sentenceSpans = sentenceAsTokenSpans.map(sentenceTokenSpans =>
      Tuple2(Span(sentenceTokenSpans.head._1.begin, sentenceTokenSpans.last._1.end), Sentence()))

    // Flatten all the tokens into a single list
    val tokenSpans = sentenceAsTokenSpans.flatMap(t=> t)

    // Create new Add annotations to a
    //if(start != slab.content.length)
    //spans += Span(start, slab.content.length) -> Token(slab.content.substring(start, slab.content.length))
    var s = slab.addLayer[Sentence](sentenceSpans).addLayer[Token](tokenSpans)
    s
    //slab
  }
}
