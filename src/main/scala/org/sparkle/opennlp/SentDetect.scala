package org.sparkle.opennlp

import java.io.InputStream

import epic.slab._
import epic.trees.Span
import opennlp.tools.sentdetect.{SentenceDetectorME, SentenceModel}
import org.sparkle.typesystem.basic.{Token}
import org.sparkle.typesystem.ops.{EpicSentenceOps, SentenceOps}

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._

abstract class OpenNlpSentenceSegmenterImplBase[SENTENCE_TYPE]( sentenceModelPath: String)
  extends StringAnalysisFunction[Any, SENTENCE_TYPE] with Serializable {

  val multipleNewlinesRegex = "(?m)\\s*\\n\\s*\\n\\s*".r
  val leadingWhitespaceRegex = "^\\s+".r
  val trailingWhitespaceRegex = "\\s+$".r

  val modelInputStream = getClass.getResource(sentenceModelPath).openStream()
  val model = new SentenceModel(modelInputStream)
  val sentenceDetector = new SentenceDetectorME(model)

  val sentenceOps: SentenceOps[SENTENCE_TYPE]

  def getSentenceOffsets(text: String) = {
    val offsets1 = for (m <-multipleNewlinesRegex.findAllMatchIn(text)) yield m.end
    val offsets2 = for (span <-  sentenceDetector.sentPosDetect(text)) yield span.getStart
    (offsets1.toList ::: offsets2.toList).sortWith((x,y) => x < y)
  }

  override def apply[In <: Any](slab: Slab[String, Span, In]): Slab[String, Span, In with SENTENCE_TYPE] = {
    // Convert slab text to an input stream and run with ClearNLP
    val sentenceOffsets = getSentenceOffsets(slab.content)
    val sentences = ListBuffer[Tuple2[Span, SENTENCE_TYPE]]()

    var begin = 0
    var end = 0
    val textOffset = 0
    val text = slab.content

    // advance the first sentence to first non-whitespace char
    leadingWhitespaceRegex.findFirstMatchIn(text).foreach(m => begin += m.group(0).length)
    for (offset <- sentenceOffsets) {
      end = offset /// offset is really the beginning of the next sentence

      val sentenceText = text.substring(begin, end)
      if (sentenceText.trim.length > 0) {
        trailingWhitespaceRegex.findFirstMatchIn(sentenceText).foreach(m => end -= m.group(0).length)
        sentences += Tuple2(Span(textOffset + begin, textOffset + end), sentenceOps.createSentence())
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
        sentences += Tuple2(Span(textOffset + begin, textOffset + end), sentenceOps.createSentence())
      }

    }

    val updated = sentenceOps.addSentences(slab, sentences)
    updated
  }

}

class OpenNlpSentenceSegmenter(sentenceModelPath: String)
  extends OpenNlpSentenceSegmenterImplBase[Sentence](sentenceModelPath) {
  override val sentenceOps: SentenceOps[Sentence] = EpicSentenceOps
}

object OpenNlpSentenceSegmenter {
  def sentenceSegmenter(sentenceModelPath: String = "/opennlp/models/en-sent.bin") =
    new OpenNlpSentenceSegmenter(sentenceModelPath)

}

object SentenceSegmenter extends epic.preprocess.SentenceSegmenter {
  // FIXME parameterize language code and pre-load tokenizer
  val sentenceModelPath = "/opennlp/models/en-sent.bin"
  val multipleNewlinesRegex = "(?m)\\s*\\n\\s*\\n\\s*".r
  val leadingWhitespaceRegex = "^\\s+".r
  val trailingWhitespaceRegex = "\\s+$".r

  val modelInputStream = getClass.getResource(sentenceModelPath).openStream()
  val model = new SentenceModel(modelInputStream)
  val sentenceDetector = new SentenceDetectorME(model)


  def getSentenceOffsets(text: String) = {
    val offsets1 = for (m <-multipleNewlinesRegex.findAllMatchIn(text)) yield m.end
    val offsets2 = for (span <-  sentenceDetector.sentPosDetect(text)) yield span.getStart
    (offsets1.toList ::: offsets2.toList).sortWith((x,y) => x < y)
  }


  override def apply[In](slab: StringSlab[In]): StringSlab[In with Sentence] = {

    // Convert slab text to an input stream and run with ClearNLP
    val sentenceOffsets = getSentenceOffsets(slab.content)
    val sentences = ListBuffer[Tuple2[Span, Sentence]]()

    var begin = 0
    var end = 0
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
}



