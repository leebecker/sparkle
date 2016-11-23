package org.sparkle.langdetect

import com.optimaize.langdetect.{DetectedLanguage, LanguageDetectorBuilder}
import com.optimaize.langdetect.ngram.NgramExtractors
import com.optimaize.langdetect.profiles.LanguageProfileReader
import org.sparkle.slate.{Slate, Span, StringAnalysisFunction}
import org.sparkle.typesystem.basic.Language
import org.sparkle.typesystem.ops.WindowOps

/**
  * Created by leebecker on 11/17/16.
  */
class LanguageDetector(segment: Boolean=false, collapse: Boolean=true, probThreshold: Double=0.6, windowOps: Option[WindowOps[_]]=None) extends StringAnalysisFunction with Serializable {

  val languageProfiles = new LanguageProfileReader().readAllBuiltIn()
  val languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard()).withProfiles(languageProfiles).build()


  def getLanguage(detectedLanguages: List[DetectedLanguage]): Option[DetectedLanguage] = {

    if (detectedLanguages.isEmpty) {
      None
    } else {
      var currLang: DetectedLanguage = detectedLanguages.head
      for (lang <- detectedLanguages.tail) {
        if (lang.getProbability > currLang.getProbability) {
          currLang = lang
        }
      }
      Some(currLang)
    }
  }

  val paragraphSplitterRegex = """\n{2,}""".r

  def detect(text: String, threshold: Double=0.5): Language = {
    import collection.JavaConverters._
    val probs = languageDetector.getProbabilities(text).asScala

    var maxProb = -1.0d
    var maxLang = "unknown"
    for (detected <- probs) {
      if (detected.getProbability > maxProb) {
        maxProb = detected.getProbability
        maxLang = detected.getLocale.getLanguage
      }
    }

    Language(maxLang, maxProb)
    if (maxProb > threshold) {
      Language(maxLang, maxProb)
    } else {
      Language()
    }
  }

  override def apply(slate: Slate[String, Span]): Slate[String, Span] = {

    if (segment) {
      val text = slate.content
      var currOffset = 0
      val spans = {
        val s1 = for (m <- paragraphSplitterRegex.findAllMatchIn(text)) yield {
          val span = Span(currOffset, m.start)
          currOffset = m.end
          span
        }

        if (currOffset < text.length - 1) {
          s1 ++ Seq(Span(currOffset, text.length))
        } else {
          s1
        }
      }

      val spansAndLanguages = spans.map {
        span =>
          val spanText = slate.spanned(span)
          (span, detect(spanText, probThreshold))
      }

      slate.addLayer[Language](spansAndLanguages)

    } else {
      val span = Span(0, slate.content.length)
      val spansAndLanguages = Seq((span, detect(slate.content, probThreshold)))
      slate.addLayer[Language](spansAndLanguages)
    }

  }
}
