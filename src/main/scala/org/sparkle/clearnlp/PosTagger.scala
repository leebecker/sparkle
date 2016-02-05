package org.sparkle.clearnlp

import edu.emory.clir.clearnlp.component.mode.pos.AbstractPOSTagger
import edu.emory.clir.clearnlp.component.utils.{GlobalLexica, NLPUtils}
import edu.emory.clir.clearnlp.dependency.DEPTree
import edu.emory.clir.clearnlp.util.lang.TLanguage
import epic.slab._
import epic.trees.Span
import org.sparkle.typesystem.basic.{Token}

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

trait PosTaggerOps[SENTENCE, TOKEN, POSTAG] extends StringAnalysisFunction[SENTENCE with TOKEN, TOKEN] {

  def getSentences[In <: SENTENCE](slab: StringSlab[In]): TraversableOnce[(Span, SENTENCE)]

  def getTokens[In <: TOKEN](slab: StringSlab[In], sentenceSpan: Span, sentence: SENTENCE): TraversableOnce[(Span, TOKEN)]

  def createPosTag[In <: SENTENCE with TOKEN](slab: StringSlab[In], tag: String, token: TOKEN): POSTAG

  def updateSlab[In <: SENTENCE with TOKEN](slab: StringSlab[In], posTags: TraversableOnce[(Span, POSTAG)]): StringSlab[In with POSTAG]
}

abstract class PosTaggerImplBase[SENTENCE, TOKEN, POSTAG](
    languageCode: String, modelPath: String, paths: Seq[String])
  extends PosTaggerOps[SENTENCE, TOKEN, POSTAG] {

  val tagger = NLPUtils.getPOSTagger(TLanguage.getType(languageCode), modelPath)
  GlobalLexica.initDistributionalSemanticsWords(paths)

  def getTagger(): AbstractPOSTagger = tagger

  override
  def apply[In <: SENTENCE with TOKEN](slab: StringSlab[In]): StringSlab[In with POSTAG] = {
    val posTaggedTokenSpans = getSentences(slab).flatMap{
      case (sentenceSpan, sentence) =>
        val tokens = getTokens(slab, sentenceSpan, sentence).toIndexedSeq
        val tokenStrings = tokens.map { case (tokenSpan, _) => slab.spanned(tokenSpan)}

        // Run ClearNLP pos tagger
        val clearNlpDepTree = new DEPTree(tokenStrings)
        getTagger().process(clearNlpDepTree)
        // Create copy of existing tokens with POS tags
        tokens.zip(clearNlpDepTree).map {
          case ((span, token), depnode) =>
            (Span(span.begin, span.end), this.createPosTag(slab, depnode.getPOSTag, token))
        }
    }

    // Strangely this needs to be split into two lines or else
    // we get a compiler error
    val resultSlab = this.updateSlab(slab, posTaggedTokenSpans)
    resultSlab
  }
}

class PosTaggerWithSparkleTypes(languageCode: String, modelPath: String, paths: Seq[String])
  extends PosTaggerImplBase[Sentence, Token, Token](languageCode, modelPath, paths) with Serializable {

  override def getSentences[In <: Sentence](slab: StringSlab[In]): TraversableOnce[(Span, Sentence)] =
    slab.iterator[Sentence]

  override
  def updateSlab[In <: Sentence with Token](slab: StringSlab[In], posTags: TraversableOnce[(Span, Token)]): StringSlab[In with Token] =
    slab.removeLayer[Token].addLayer[Token](posTags)

  override def createPosTag[In <: Sentence with Token](slab: StringSlab[In], tag: String, token: Token): Token =
    token.copy(pos=Option(tag))

  override def getTokens[In <: Token](slab: StringSlab[In], sentenceSpan: Span, sentence: Sentence): TraversableOnce[(Span, Token)] =
    slab.covered[Token](sentenceSpan)
}


class PosTaggerWithEpicTypes(languageCode: String, modelPath: String, paths: Seq[String])
  extends PosTaggerImplBase[Sentence, Token, PartOfSpeech](languageCode, modelPath, paths) with Serializable {

  override def getSentences[In <: Sentence](slab: StringSlab[In]): TraversableOnce[(Span, Sentence)] =
    slab.iterator[Sentence]

  override
  def updateSlab[In <: Sentence with Token](slab: StringSlab[In], posTags: TraversableOnce[(Span, PartOfSpeech)]): StringSlab[In with PartOfSpeech] =
    slab.addLayer[PartOfSpeech](posTags)

  override def createPosTag[In <: Sentence with Token](slab: StringSlab[In], tag: String, token: Token): PartOfSpeech =
  PartOfSpeech(tag, None)
    //token.copy(pos=Option(tag))

  override def getTokens[In <: Token](slab: StringSlab[In], sentenceSpan: Span, sentence: Sentence): TraversableOnce[(Span, Token)] =
    slab.covered[Token](sentenceSpan)
}




object PosTagger2 {
  def sparkleTypesPosTagger(
    languageCode: String=TLanguage.ENGLISH.toString,
    modelPath: String = "general-en-pos.xz",
    paths: Seq[String] = "brown-rcv1.clean.tokenized-CoNLL03.txt-c1000-freq1.txt.xz" :: Nil
  ) = new PosTaggerWithSparkleTypes(languageCode, modelPath, paths)

  def epicTypesPosTagger(
    languageCode: String=TLanguage.ENGLISH.toString,
    modelPath: String = "general-en-pos.xz",
    paths: Seq[String] = "brown-rcv1.clean.tokenized-CoNLL03.txt-c1000-freq1.txt.xz" :: Nil
  ) = new PosTaggerWithEpicTypes(languageCode, modelPath, paths)

}
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
