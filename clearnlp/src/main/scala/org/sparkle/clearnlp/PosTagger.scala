package org.sparkle.clearnlp

import edu.emory.clir.clearnlp.component.mode.pos.AbstractPOSTagger
import edu.emory.clir.clearnlp.component.utils.{GlobalLexica, NLPUtils}
import edu.emory.clir.clearnlp.dependency.DEPTree
import edu.emory.clir.clearnlp.util.lang.TLanguage
import org.sparkle.slate._
import org.sparkle.typesystem.basic.{Sentence,Token}
import org.sparkle.typesystem.ops._

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

abstract class PosTaggerImplBase[SENTENCE, TOKEN, POSTAG](
    languageCode: String, modelPath: String, paths: Seq[String])
  extends StringAnalysisFunction with Serializable {

  val sentenceOps: SentenceOps[SENTENCE]
  val tokenOps: TokenOps[TOKEN]
  val posTagOps: PartOfSpeechOps[TOKEN, POSTAG]

  val tagger = NLPUtils.getPOSTagger(TLanguage.getType(languageCode), modelPath)
  GlobalLexica.initDistributionalSemanticsWords(paths)

  def getTagger(): AbstractPOSTagger = tagger

  override
  def apply(slate: StringSlate): StringSlate = {
    val posTaggedTokenSpans = sentenceOps.selectAllSentences(slate).flatMap{
      case (sentenceSpan, sentence) =>
        val tokens = tokenOps.selectTokens(slate, sentenceSpan).toIndexedSeq
        val tokenStrings = tokens.map { case (tokenSpan, _) => slate.spanned(tokenSpan)}

        // Run ClearNLP pos tagger
        val clearNlpDepTree = new DEPTree(tokenStrings)
        getTagger().process(clearNlpDepTree)
        // Create copy of existing tokens with POS tags
        tokens.zip(clearNlpDepTree).map {
          case ((span, token), depnode) =>
            (Span(span.begin, span.end), posTagOps.createPosTag(depnode.getPOSTag, token))
        }
    }

    // Strangely this needs to be split into two lines or else
    // we get a compiler error
    val resultSlate = posTagOps.addPosTags(slate, posTaggedTokenSpans)
    resultSlate
  }

}

class PosTaggerWithSparkleTypes(languageCode: String, modelPath: String, paths: Seq[String])
  extends PosTaggerImplBase[Sentence, Token, Token](languageCode, modelPath, paths) {

  override val sentenceOps: SentenceOps[Sentence] = SparkleSentenceOps
  override val tokenOps: TokenOps[Token] = SparkleTokenOps
  override val posTagOps: PartOfSpeechOps[Token, Token] = SparklePartOfSpeechOps
}


/*
class PosTaggerWithEpicTypes(languageCode: String, modelPath: String, paths: Seq[String])
  extends PosTaggerImplBase[epic.slab.Sentence, epic.slab.Token, epic.slab.PartOfSpeech](languageCode, modelPath, paths) {

  override val sentenceOps: SentenceOps[epic.slab.Sentence] = EpicSentenceOps
  override val tokenOps: TokenOps[epic.slab.Token] = EpicTokenOps
  override val posTagOps: PartOfSpeechOps[epic.slab.Token, epic.slab.PartOfSpeech] = EpicPartOfSpeechOps
}
*/


//object PosTagger {
//  def sparkleTypesPosTagger(
//    languageCode: String=TLanguage.ENGLISH.toString,
//    modelPath: String = "general-en-pos.xz",
//    paths: Seq[String] = "brown-rcv1.clean.tokenized-CoNLL03.txt-c1000-freq1.txt.xz" :: Nil
//  ) = new PosTaggerWithSparkleTypes(languageCode, modelPath, paths)

  /*
  def epicTypesPosTagger(
    languageCode: String=TLanguage.ENGLISH.toString,
    modelPath: String = "general-en-pos.xz",
    paths: Seq[String] = "brown-rcv1.clean.tokenized-CoNLL03.txt-c1000-freq1.txt.xz" :: Nil
  ) = new PosTaggerWithEpicTypes(languageCode, modelPath, paths)
  */

//}

