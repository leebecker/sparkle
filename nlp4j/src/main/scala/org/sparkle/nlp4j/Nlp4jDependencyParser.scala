package org.sparkle.nlp4j

import edu.emory.mathcs.nlp.common.util.Language
import edu.emory.mathcs.nlp.component.dep.DEPState
import edu.emory.mathcs.nlp.component.template.OnlineComponent
import edu.emory.mathcs.nlp.component.template.node.NLPNode
import edu.emory.mathcs.nlp.decode.NLPUtils
import org.sparkle.slate._
import org.sparkle.typesystem.basic.{Sentence, Token}
import org.sparkle.typesystem.ops._
import org.sparkle.typesystem.ops.sparkle._
import org.sparkle.typesystem.syntax.dependency.{DependencyNode, DependencyRelation, LeafDependencyNode, RootDependencyNode}

/**
  * Created by leebecker on 6/15/16.
  */
abstract class Nlp4jDependencyParserImplBase[SENTENCE, TOKEN, POSTAG, LEMMA, NODE, TOKEN_NODE<:NODE, ROOT_NODE <: NODE, RELATION](language: Language, modelPath: String)
  extends StringAnalysisFunction with Serializable {
  require(language == Language.ENGLISH, s"Language $language unsupported in Sparkle NLP4j POS Tagger Wrapper.")

  val sentenceOps: SentenceOps[SENTENCE]
  val tokenOps: TokenOps[TOKEN]
  val lemmaOps: LemmaOps[TOKEN, LEMMA]
  val posTagOps: PartOfSpeechOps[TOKEN, POSTAG]
  val dependencyOps: DependencyOps[TOKEN, NODE, TOKEN_NODE, ROOT_NODE, RELATION]

  Nlp4jUtils.initWordClusters()
  lazy val parser = NLPUtils.getComponent(getClass.getResourceAsStream(modelPath)).asInstanceOf[OnlineComponent[NLPNode, DEPState[NLPNode]]]

  override def apply(slate: StringSlate): StringSlate = {

    val sentences = sentenceOps.selectAllSentences(slate)
    var slateOut = slate

    sentences.foreach {
      case (sentenceSpan, sentence) =>
        val tokens = tokenOps.selectTokens(slate, sentenceSpan).toIndexedSeq
        val tokenStrings = tokens.map(t => tokenOps.getText(slate, t._1, t._2))
        val posTags = posTagOps.selectPosTags(slate, sentenceSpan).toIndexedSeq
        val posTagStrings = posTags.map(p => posTagOps.getPosTag(slate, p._1, p._2).orNull)

        val rootNode = new NLPNode()
        rootNode.toRoot()
        rootNode.setStartOffset(sentenceSpan.begin)
        rootNode.setEndOffset(sentenceSpan.end)


        // Dress up NLPNodes with token info for processing by tagger
        val tokenNodes = tokens.zipWithIndex.map {
          case ((tokenSpan, token), tokenIdx) =>
            val pos = posTagOps.getPos(slate, tokenSpan)
            val posTag = posTagOps.getPosTag(slate, tokenSpan, pos).orNull
            val lemma = lemmaOps.getLemma(slate, tokenSpan)
            val lemmaLabel = lemmaOps.getLemmaText(slate, tokenSpan, lemma).orNull
            val tokenText = tokenOps.getText(slate, tokenSpan, token)
            val node = new NLPNode(tokenIdx+1, tokenText, posTag)
            node.setLemma(lemmaLabel)
            node.setStartOffset(tokenSpan.begin)
            node.setEndOffset(tokenSpan.end)
            node
        }
        val nodes = Array(rootNode) ++ tokenNodes
        Nlp4jUtils.lexica.process(nodes)

        parser.process(nodes)

        // now pull out the nodes and build a dependency graph
        val (depNodes, depRels) = convertToDependencyGraph(tokens.map(_._2), nodes)
        slateOut = dependencyOps.addNodes(slateOut, depNodes)
        slateOut = dependencyOps.addRelations(slateOut, depRels)
    }
    slateOut
  }

  def convertToDependencyGraph(tokens: IndexedSeq[TOKEN], nlp4jNodes: IndexedSeq[NLPNode]) = {

    // Create nodes
    val rootNode = dependencyOps.createRootNode(Span(nlp4jNodes.head.getStartOffset, nlp4jNodes.head.getEndOffset))
    val tokenNodes = (tokens zip nlp4jNodes.tail).map {
      case (token, node) =>
      val span = Span(node.getStartOffset, node.getEndOffset)
      dependencyOps.createNode(span, token)
    }

    val depNodes = IndexedSeq(rootNode) ++ tokenNodes

    // Now create relations
    val depRels = nlp4jNodes.zipWithIndex.flatMap {
      case (nlpnode, 0) => None
      case (nlpnode, idx) =>
        val head = nlpnode.getDependencyHead
        val childDepNode = depNodes(idx)
        val headDepNode = depNodes(head.getID)
        val relation = Some(dependencyOps.createRelation(childDepNode, headDepNode, nlpnode.getDependencyLabel))
        dependencyOps.linkNodes(childDepNode, headDepNode, relation.get)
        relation
    }
    (depNodes, depRels)
  }
}

class Nlp4jDependencyParserWithSparkleTypes(language: Language, modelPath: String)
  extends Nlp4jDependencyParserImplBase[Sentence, Token, Token, Token, DependencyNode, LeafDependencyNode, RootDependencyNode, DependencyRelation](language, modelPath) {

  override val sentenceOps: SentenceOps[Sentence] = SparkleSentenceOps
  override val posTagOps: PartOfSpeechOps[Token, Token] = SparklePartOfSpeechOps
  override val lemmaOps: LemmaOps[Token, Token] = SparkleLemmaOps
  override val tokenOps: TokenOps[Token] = SparkleTokenOps
  override val dependencyOps: DependencyOps[Token, DependencyNode, LeafDependencyNode, RootDependencyNode, DependencyRelation] = SparkleDependencyOps
}
