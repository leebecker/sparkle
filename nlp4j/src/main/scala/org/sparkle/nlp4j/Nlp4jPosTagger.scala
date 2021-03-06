package org.sparkle.nlp4j

import edu.emory.mathcs.nlp.common.util.{IOUtils, Language, NLPUtils}
import edu.emory.mathcs.nlp.component.pos.POSState
import edu.emory.mathcs.nlp.component.template.OnlineComponent
import edu.emory.mathcs.nlp.component.template.node.NLPNode
import org.sparkle.slate._
import org.sparkle.typesystem.basic.{Sentence, Token}
import org.sparkle.typesystem.ops._
import org.sparkle.typesystem.ops.sparkle.{SparklePartOfSpeechOps, SparkleSentenceOps, SparkleTokenOps}

/**
  *
  * Base class for NLP4J pos tagger wrapper.  Override the ops to support different typesystems.
  *
  * @param language language code.  Currently only English is supported
  * @param modelPath path to model file
  * @tparam SENTENCE annotation type containing sentence information
  * @tparam TOKEN annotation type containing token information
  * @tparam POSTAG annotation type containing POS tag information
  */
abstract class Nlp4jPosTaggerImplBase[SENTENCE, TOKEN, POSTAG](language: Language, modelPath: String, windowOps: Option[WindowOps[_]]=None)
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
    val sentences = if (windowOps.isEmpty) {
      // Get all sentences
      sentenceOps.selectAllSentences(slate)
    } else {
      // Get sentences covered by relevant windows
      windowOps.get.selectWindows(slate).flatMap{
        case (windowSpan, window) => sentenceOps.selectSentences(slate, windowSpan)
      }
    }

    val posTaggedTokenSpans = sentences.flatMap {
      case (sentenceSpan, sentence) =>
        val tokens = tokenOps.selectTokens(slate, sentenceSpan).toIndexedSeq
        val tokenStrings = tokens.map(t => tokenOps.getText(slate, t._1, t._2))
          tokens.map { case (tokenSpan, _) => slate.spanned(tokenSpan) }
        val rootNode = new NLPNode()
        rootNode.toRoot()

        // Dress up NLPNodes with token info for processing by tagger
        val tokenNodes = (tokens zip tokenStrings).zipWithIndex.map {
          case ((tokenSpan, tokenText), tokenIdx) => new NLPNode(tokenIdx, tokenText)
        }
        val sentenceNodes = Array(rootNode) ++ tokenNodes
        Nlp4jUtils.lexica.process(sentenceNodes)

        // Run nlp4j POS tagger
        tagger.process(sentenceNodes)

        // Convert nlp4j output to slate POS tags
        (tokens zip tokenNodes).map {
          case ((span, token), node) => (span, posTagOps.createPosTag(node.getPartOfSpeechTag, token))
        }
    }

    posTagOps.addPosTags(slate, posTaggedTokenSpans)
  }
}

/**
  * Define POS tagger for Sparkle TypeSystem
  * @param language language code for defining models
  * @param modelPath path to model file
  */
class Nlp4jPosTaggerWithSparkleTypes(language: Language, modelPath: String, windowOps: Option[WindowOps[_]]=None)
    extends Nlp4jPosTaggerImplBase[Sentence, Token, Token](language, modelPath, windowOps) {

    override val sentenceOps: SentenceOps[Sentence] = SparkleSentenceOps
    override val tokenOps: TokenOps[Token] = SparkleTokenOps
    override val posTagOps: PartOfSpeechOps[Token, Token] = SparklePartOfSpeechOps
}

