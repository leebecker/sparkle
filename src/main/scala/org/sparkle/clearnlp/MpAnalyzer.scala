package org.sparkle.clearnlp


import edu.emory.clir.clearnlp.component.utils.{GlobalLexica, NLPUtils}
import edu.emory.clir.clearnlp.dependency.DEPTree
import edu.emory.clir.clearnlp.util.lang.TLanguage
import org.sparkle.slate._
import org.sparkle.typesystem.basic.{Sentence,Token}

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

/**
  * SparkLE wrapper for ClearNLP MpAnalyzer<p>
  *
  * Prerequisites: Slate object with Sentence and Token annotations <br>
  * Outputs: new Slate object with Sentence and Tokens with pos field set <br>
  */
object MpAnalyzer extends StringAnalysisFunction with Serializable {
  val defaultLanguageCode = TLanguage.ENGLISH.toString
  val mpAnalyzer = NLPUtils.getMPAnalyzer(TLanguage.getType(defaultLanguageCode));

  def apply(slate: StringSlate): StringSlate =  {
    val lemmatizedTaggedTokenSpans = slate.iterator[Sentence].flatMap{
      case(sentenceSpan, _) =>
        val tokens = slate.covered[Token](sentenceSpan)
        val tokenStrings = tokens.map { case (tokenSpan, _) => slate.spanned(tokenSpan) }

        // Run ClearNLP pos tagger
        val clearNlpDepTree = new DEPTree(tokenStrings)
        for (((span, token), idx) <- tokens.zipWithIndex) {
          clearNlpDepTree.get(idx+1).setPOSTag(token.pos.get)
        }
        mpAnalyzer.process(clearNlpDepTree)
        // Create copy of existing tokens with POS tags
        tokens.zip(clearNlpDepTree).map {
          case ((span, token), depnode) => (Span(span.begin, span.end), token.copy(lemma=Option(depnode.getLemma)))
        }
    }

    // Remove old tokens and add new pos-tagged ones to slate
    // FIXME - potentially dangerous if operating over specialized windows
    val res = slate.removeLayer[Token].addLayer[Token](lemmatizedTaggedTokenSpans)
    res
  }

}
