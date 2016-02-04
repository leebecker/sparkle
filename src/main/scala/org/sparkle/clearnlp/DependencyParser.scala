package org.sparkle.clearnlp

import edu.emory.clir.clearnlp.component.mode.dep.DEPConfiguration
import edu.emory.clir.clearnlp.component.utils.{GlobalLexica, NLPUtils}
import edu.emory.clir.clearnlp.dependency.{DEPFeat, DEPNode, DEPTree}
import edu.emory.clir.clearnlp.util.lang.TLanguage
import epic.slab._
import epic.trees.Span
import org.sparkle.typesystem.basic.{Token}
import org.sparkle.typesystem.syntax.dependency._

import scala.collection.JavaConversions._
import scala.collection.mutable

/**
  * SparkLE wrapper for ClearNLP Dependency Parser<p>
  *
  * Prerequisites: Slab object with Sentence and Token annotations <br>
  * Outputs: new Slab object with Sentence and Tokens with pos field set <br>
  */
object DependencyParser extends StringAnalysisFunction[Sentence with Token, DependencyNode with DependencyRelation] with Serializable {
  val defaultLanguageCode = TLanguage.ENGLISH.toString
  val mpAnalyzer = NLPUtils.getMPAnalyzer(TLanguage.getType(defaultLanguageCode))
  val parserModelPath = "general-en-dep.xz"

  GlobalLexica.initDistributionalSemanticsWords(PosTagger.paths)
  val parser = NLPUtils.getDEPParser(TLanguage.getType(defaultLanguageCode), parserModelPath, new DEPConfiguration("root"))

  def apply[In <: Token with Sentence](slab: StringSlab[In]): StringSlab[In with DependencyNode with DependencyRelation] = {
    val depGraphSpans = slab.iterator[Sentence].map {
      case (sentenceSpan, sentence) =>
        val tokens = slab.covered[Token](sentenceSpan).seq
        val tokenStrings = tokens.map { case (tokenSpan, _) => slab.spanned(tokenSpan) }

        val tree = new DEPTree(tokens.length)
        for (((span, token), i) <- tokens.zipWithIndex) {
          val node = new DEPNode(i + 1, slab.spanned(span), token.pos.get, token.lemma.get, new DEPFeat())
          tree.add(node)
        }
        val a  = slab.iterator[Token]
        this.parser.process(tree)
        val (nodes, relations) = depTreeToDepGraph((sentenceSpan, sentence), tokens, tree)
        nodes.zip(relations)
        depTreeToDepGraph((sentenceSpan, sentence), tokens, tree)
    }.toList
    val nodes = depGraphSpans.flatMap(x => x._1)
    val relations = depGraphSpans.flatMap(x => x._2)
    val s = slab.addLayer[DependencyNode](nodes).addLayer[DependencyRelation](relations)
    s
  }

  def depTreeToDepGraph(sentence: (Span, Sentence), tokens: Seq[(Span, Token)], tree: DEPTree)  = {
    val nodes = IndexedSeq[DependencyNode](RootDependencyNode(sentence._2)) ++ tokens.map{case (span, token) => TokenDependencyNode(Option(token), Option(span))}
    val nodeSpans = IndexedSeq(Span(sentence._1.begin, sentence._1.end)) ++ tokens.map{case(span, token) => Span(span.begin, span.end)}

    val relations = tree.map { node =>
      val head = node.getHead
      val headSpan = nodeSpans(head.getID)
      val nodeSpan = nodeSpans(node.getID)
      val begin = math.min(nodeSpan.begin, headSpan.begin)
      val end = math.max(nodeSpan.begin, headSpan.begin)

      (Span(begin, end), DependencyUtils.linkDependencyNodes(node.getLabel, nodes(node.getID), nodes(head.getID)))
    }
    (nodeSpans.zip(nodes), relations)
  }
}
