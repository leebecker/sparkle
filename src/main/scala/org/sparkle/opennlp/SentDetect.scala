package org.sparkle.opennlp

import java.io.InputStream

import edu.emory.clir.clearnlp.component.utils.NLPUtils
import edu.emory.clir.clearnlp.util.lang.TLanguage
import epic.slab.{StringSlab, Slab, StringAnalysisFunction}
import epic.trees.Span
import opennlp.tools.sentdetect.{SentenceDetectorME, SentenceModel}
import org.sparkle.typesystem.basic.{Token, Sentence}

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._

trait SentenceSegmenterBase extends StringAnalysisFunction[Any, Sentence] with (String => Iterable[String]) with Serializable {
  override def toString = getClass.getName

  def apply(a: String):IndexedSeq[String] = {
    val slab = Slab(a)
    apply(slab).iterator[Sentence].toIndexedSeq.map(s => slab.spanned(s._1))
  }

}

object SentenceSegmenter extends SentenceSegmenterBase {
  // FIXME parameterize language code and pre-load tokenizer
  val defaultLanguageCode = TLanguage.ENGLISH.toString
  val tokenizer = NLPUtils.getTokenizer(TLanguage.getType(defaultLanguageCode))
  val sentenceModelPath = "/opennlp/models/en-sent.bin"
  val multipleNewlinesRegex = "(?m)\\s*\\n\\s*\\n\\s*".r
  val leadingWhitespaceRegex = "^\\s+".r
  val trailingWhitespaceRegex = "\\s+$".r

  val modelInputStream = getClass.getResource(sentenceModelPath).openStream()
  val model = new SentenceModel(modelInputStream)
  val sentenceDetector = new SentenceDetectorME(model);


  def getSentenceOffsets(text: String) = {
    val offsets1 = for (m <-multipleNewlinesRegex.findAllMatchIn(text)) yield m.end
    val offsets2 = for (span <-  sentenceDetector.sentPosDetect(text)) yield span.getStart
    (offsets1.toList ::: offsets2.toList).sortWith((x,y) => x < y)
  }


  override def apply[In](slab: StringSlab[In]): StringSlab[In with Sentence] = {

    // Convert slab text to an input stream and run with ClearNLP
    val sentenceOffsets = getSentenceOffsets(slab.content)
    val sentences = ListBuffer[Tuple2[Span, Sentence]]()

    var begin = 0;
    var end = 0;
    val textOffset = 0
    val text = slab.content

    // advance the first sentence to first non-whitespace char
    leadingWhitespaceRegex.findFirstMatchIn(text).foreach(m => begin += m.group(0).length)
    for (offset <- sentenceOffsets) {
      end = offset /// offset is really the beginning of the next sentence

      val sentenceText = text.substring(begin, end)
      if (sentenceText.trim.length > 0) {
        trailingWhitespaceRegex.findFirstMatchIn(sentenceText).foreach(m => end -= m.group(0).length)
        sentences += Tuple2(Span(textOffset + begin, textOffset + end), Sentence())
      }
      begin = offset

    }
    // take the remaining text if there is any and add it to a sentence.
    // this code will not execute if the text ends with a sentence detected by
    // SentenceDetector because it actually returns an offset corresponding to the end
    // of the last sentence (see note on getSentenceOffsets)
    if (begin < text.length) {
      val sentenceText = text.substring(begin, text.length)
      end = text.length
      if (sentenceText.trim.length > 0) {
        trailingWhitespaceRegex.findFirstMatchIn(sentenceText).foreach(m => end -= m.group(0).length)
        sentences += Tuple2(Span(textOffset + begin, textOffset + end), Sentence())
      }

    }



    slab.addLayer[Sentence](sentences)
  }


//    // Convert token strings in each sentence into Token span annotations
//    val sentenceAsTokenSpans = sentencesAsTokens.map(sentenceTokens => {
//      var offset = 0
//      slab.content.indexOf()
//      val tokens = new ListBuffer[Tuple2[Span, Token]]()
//      for (tokenString: String <- sentenceTokens) {
//        val tokenBegin = slab.content.indexOf(tokenString, offset)
//        val tokenEnd = tokenBegin + tokenString.length
//        if (tokenBegin >= 0 && tokenEnd >= 0) {
//          tokens += Tuple2(Span(tokenBegin, tokenEnd), Token(slab.content.substring(tokenBegin, tokenEnd)))
//        }
//        offset = tokenEnd
//      }
//      tokens.toList
//    })
//
//    // Take the head and tail of each sentence to get the sentence offsets
//    val sentenceSpans = sentenceAsTokenSpans.map(sentenceTokenSpans =>
//      Tuple2(Span(sentenceTokenSpans.head._1.begin, sentenceTokenSpans.last._1.end), Sentence()))
//
//    // Create new Add annotations to a
//    var s = slab.addLayer[Sentence](sentenceSpans)
//    s
//  }
}



