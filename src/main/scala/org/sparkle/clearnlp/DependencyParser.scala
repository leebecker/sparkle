package org.sparkle.clearnlp

import edu.emory.clir.clearnlp.component.mode.dep.DEPConfiguration
import edu.emory.clir.clearnlp.component.utils.{GlobalLexica, NLPUtils}
import edu.emory.clir.clearnlp.dependency.{DEPFeat, DEPNode, DEPTree}
import edu.emory.clir.clearnlp.util.lang.TLanguage
import org.sparkle.slate._
import org.sparkle.typesystem.basic.{Sentence,Token}
import org.sparkle.typesystem.syntax.dependency._

import scala.collection.JavaConversions._
import scala.collection.mutable

/**
  * SparkLE wrapper for ClearNLP Dependency Parser<p>
  *
  * Prerequisites: Slate object with Sentence and Token annotations <br>
  * Outputs: new Slate object with Sentence and Tokens with pos field set <br>
  */
object DependencyParser extends StringAnalysisFunction with Serializable {
  val defaultLanguageCode = TLanguage.ENGLISH.toString
  val mpAnalyzer = NLPUtils.getMPAnalyzer(TLanguage.getType(defaultLanguageCode))
  val parserModelPath = "general-en-dep.xz"

  val paths: Seq[String] = "brown-rcv1.clean.tokenized-CoNLL03.txt-c1000-freq1.txt.xz" :: Nil
  GlobalLexica.initDistributionalSemanticsWords(paths)
  val parser = NLPUtils.getDEPParser(TLanguage.getType(defaultLanguageCode), parserModelPath, new DEPConfiguration("root"))

  def apply(slate: StringSlate): StringSlate = {
    val depGraphSpans = slate.iterator[Sentence].map {
      case (sentenceSpan, sentence) =>
        val tokens = slate.covered[Token](sentenceSpan).seq
        val tokenStrings = tokens.map { case (tokenSpan, _) => slate.spanned(tokenSpan) }

        val tree = new DEPTree(tokens.length)
        for (((span, token), i) <- tokens.zipWithIndex) {
          val node = new DEPNode(i + 1, slate.spanned(span), token.pos.get, token.lemma.get, new DEPFeat())
          tree.add(node)
        }
        val a  = slate.iterator[Token]
        this.parser.process(tree)
        val (nodes, relations) = depTreeToDepGraph((sentenceSpan, sentence), tokens, tree)
        nodes.zip(relations)
        depTreeToDepGraph((sentenceSpan, sentence), tokens, tree)
    }.toList
    val nodes = depGraphSpans.flatMap(x => x._1)
    val relations = depGraphSpans.flatMap(x => x._2)
    val s = slate.addLayer[DependencyNode](nodes).addLayer[DependencyRelation](relations)
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
