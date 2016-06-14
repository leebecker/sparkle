package org.sparkle.nlp4j

import edu.emory.mathcs.nlp.common.util.{IOUtils, Language}
import edu.emory.mathcs.nlp.component.pos.POSState
import edu.emory.mathcs.nlp.component.template.OnlineComponent
import edu.emory.mathcs.nlp.component.template.node.NLPNode
import edu.emory.mathcs.nlp.decode.NLPUtils
import org.sparkle.slate._
import org.sparkle.typesystem.basic.{Sentence, Token}
import org.sparkle.typesystem.ops._

/**
  * Created by leebecker on 6/14/16.
  *
  * Base class for NLP4J pos tagger wrapper.  Override the ops to support different typesystems.
  *
  */
abstract class PosTaggerImplBase[SENTENCE, TOKEN, POSTAG](language: Language, modelPath: String)
    extends StringAnalysisFunction with Serializable {

  require(language == Language.ENGLISH, s"Language $language unsupported in Sparkle NLP4j POS Tagger Wrapper.")

  val sentenceOps: SentenceOps[SENTENCE]
  val tokenOps: TokenOps[TOKEN]
  val posTagOps: PartOfSpeechOps[TOKEN, POSTAG]

  // Init POS tag relevant Global Lexica - Lazy loading will prevent us from repeated initialization
  Nlp4jUtils.initAmbiguityClasses()
  Nlp4jUtils.initWordClusters()
  lazy val tagger = NLPUtils.getComponent(getClass.getResourceAsStream(modelPath)).asInstanceOf[OnlineComponent[NLPNode, POSState[NLPNode]]]

  override def apply(slate: StringSlate): StringSlate = {
    val sentences = sentenceOps.selectAllSentences(slate)

    val posTaggedTokenSpans = sentences.flatMap {
      case (sentenceSpan, sentence) =>
        val tokens = tokenOps.selectTokens(slate, sentenceSpan).toIndexedSeq
        val tokenStrings = tokens.map { case (tokenSpan, _) => slate.spanned(tokenSpan) }
        val rootNode = new NLPNode()
        rootNode.toRoot()

        // Dress up NLPNodes with token info for processing by tagger
        val tokenNodes = (tokens zip tokenStrings).zipWithIndex.map {
          case ((tokenSpan, tokenText), tokenIdx) => new NLPNode(tokenIdx, tokenText)
        }
        val sentenceNodes = Array(rootNode) ++ tokenNodes
        // Run nlp4j POS tagger
        tagger.process(sentenceNodes)

        // Convert nlp4j output to slate POS tags
        (tokens zip tokenNodes).map {
          case ((span, token), node) => (span, posTagOps.createPosTag(node.getPartOfSpeechTag, token))
        }
    }
    // Strangely this needs to be split into two lines or else
    // we get a compiler error
    //  val resultSlate = posTagOps.addPosTags(slate, posTaggedTokenSpans)
    //resultSlate
    posTagOps.addPosTags(slate, posTaggedTokenSpans)

  }
}

class PosTaggerWithSparkleTypes(language: Language, modelPath: String)
    extends PosTaggerImplBase[Sentence, Token, Token](language, modelPath) {

    override val sentenceOps: SentenceOps[Sentence] = SparkleSentenceOps
    override val tokenOps: TokenOps[Token] = SparkleTokenOps
    override val posTagOps: PartOfSpeechOps[Token, Token] = SparklePartOfSpeechOps
}

