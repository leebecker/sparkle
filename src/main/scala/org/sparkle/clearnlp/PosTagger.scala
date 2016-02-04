package org.sparkle.clearnlp

import edu.emory.clir.clearnlp.component.utils.{GlobalLexica, NLPUtils}
import edu.emory.clir.clearnlp.dependency.DEPTree
import edu.emory.clir.clearnlp.util.lang.TLanguage
import epic.slab._
import epic.trees.Span
import org.sparkle.typesystem.basic.{Token}

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._


/**
  * SparkLE wrapper for ClearNLP POS Tagger <p>
  *
  * Prerequisites: Slab object with Sentence and Token annotations <br>
  * Outputs: new Slab object with Sentence and Tokens with pos field set <br>
  */
object PosTagger extends StringAnalysisFunction[Sentence with Token, Token] with Serializable {
  val defaultLanguageCode = TLanguage.ENGLISH.toString
  val defaultModelPath = "general-en-pos.xz"
  val defaultWindow = classOf[Sentence]
  val tagger = NLPUtils.getPOSTagger(TLanguage.getType(defaultLanguageCode), defaultModelPath)
  val paths = "brown-rcv1.clean.tokenized-CoNLL03.txt-c1000-freq1.txt.xz" :: Nil
  GlobalLexica.initDistributionalSemanticsWords(paths)

  def apply[In <: Token with Sentence](slab: StringSlab[In]): StringSlab[In with Token] =  {
    val posTaggedTokenSpans = slab.iterator[Sentence].flatMap{
      case(sentenceSpan, _) =>
        val tokens = slab.covered[Token](sentenceSpan)
        val tokenStrings = tokens.map { case (tokenSpan, _) => slab.spanned(tokenSpan) }

        // Run ClearNLP pos tagger
        val clearNlpDepTree = new DEPTree(tokenStrings)
        tagger.process(clearNlpDepTree)
        // Create copy of existing tokens with POS tags
        tokens.zip(clearNlpDepTree).map {
          case ((span, token), depnode) => (Span(span.begin, span.end), token.copy(pos=Option(depnode.getPOSTag)))
        }
    }

    // Remove old tokens and add new pos-tagged ones to slab
    // FIXME - potentially dangerous if operating over specialized windows
    val res = slab.removeLayer[Token].addLayer[Token](posTaggedTokenSpans)
    res
  }

}
