package org.sparkle.clearnlp

import java.io.InputStream

import edu.emory.clir.clearnlp.component.utils.NLPUtils
import edu.emory.clir.clearnlp.tokenization.AbstractTokenizer
import edu.emory.clir.clearnlp.util.lang.TLanguage
import org.apache.commons.io.IOUtils
import org.sparkle.slab.Slab.StringSlab
import org.sparkle.typesystem.Span
import org.sparkle.typesystem.basic.{Token, Sentence}

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._

/**
  * Created by leebecker on 1/7/16.
  */

object ClearNlp {
  // FIXME parameterize language code and pre-load tokenizer
  val languageCode = "ENGLISH"
  val tokenizer = NLPUtils.getTokenizer(TLanguage.getType(languageCode));

  def sentenceSegmenterAndTokenizer[AnnotationTypes <: Span](slab: StringSlab[AnnotationTypes]) = {
    // Convert slab text to an input stream and run with ClearNLP
    val stream: InputStream = IOUtils.toInputStream(slab.content);
    val sentencesAsTokens = tokenizer.segmentize(stream)

    // Convert token strings in each sentence into Token span annotations
    val sentenceAsTokenSpans = sentencesAsTokens.map(sentenceTokens => {
      var offset = 0
      slab.content.indexOf()
      val tokens = new ListBuffer[Token]()
      for (tokenString:String <- sentenceTokens) {
        val tokenBegin = slab.content.indexOf(tokenString, offset)
        val tokenEnd = tokenBegin + tokenString.length
        if (tokenBegin >= 0 && tokenEnd >= 0) {
          tokens += Token(tokenBegin, tokenEnd)
        }
        offset = tokenEnd
      }
      tokens.toList
    })

    // Take the head and tail of each sentence to get the sentence offsets
    val sentenceSpans = sentenceAsTokenSpans.map(sentenceTokenSpans => Sentence(sentenceTokenSpans.head.begin,sentenceTokenSpans.last.end))
    val tokenSpans = sentenceAsTokenSpans.flatMap(t=> t)

    // Create new Add annotations to a
    slab ++ sentenceSpans.iterator ++ tokenSpans.iterator
  }
}
