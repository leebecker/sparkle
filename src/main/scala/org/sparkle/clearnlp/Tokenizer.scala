package org.sparkle.clearnlp

import java.io.InputStream

import edu.emory.clir.clearnlp.component.utils.NLPUtils
import edu.emory.clir.clearnlp.util.lang.TLanguage
import org.sparkle.slab._
import org.apache.commons.io.IOUtils
import org.sparkle.typesystem.basic.{Token, Sentence, Span}

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._

/**
  * Created by leebecker on 1/7/16.
  */

trait SentenceSegmenterAndTokenizerBase extends StringAnalysisFunction[Any, Sentence with Token] with (String => Iterable[String]) with Serializable {
  override def toString = getClass.getName

  def apply(a: String):IndexedSeq[String] = {
    val slab = Slab(a)
    apply(slab).iterator[Sentence with Token].toIndexedSeq.map(s => slab.spanned(s._1))
  }
}

object SentenceSegmenterAndTokenizer extends SentenceSegmenterAndTokenizerBase {
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
