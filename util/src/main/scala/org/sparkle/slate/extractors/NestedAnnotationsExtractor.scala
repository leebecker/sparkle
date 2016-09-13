package org.sparkle.slate.extractors

import org.sparkle.slate._
import org.sparkle.slate.StringSlateExtractor
import org.sparkle.typesystem.basic.{Sentence, Token}
import org.sparkle.typesystem.syntax.dependency._


// By convention -1 is the top node
case class NestedDependencyRelationSchema(relation:String, tokenIdx: Int, headTokenIdx: Int)

case class NestedTokenSchema(token: String, pos: String, lemma: String, beginIndex: Int, endIndex: Int)

case class NestedSentenceSchema(beginIndex: Int, endIndex: Int, tokens: Seq[NestedTokenSchema], dependencyParse: Seq[NestedDependencyRelationSchema])

case class NestedAnnotations(sentences: Seq[NestedSentenceSchema])


/**
  * Extractor for pulling out slate annotations into a nested structure in the form of
  * [[org.sparkle.slate.extractors.NestedAnnotations NestedAnnotations]].
  *
  * Within a dataframe the schema looks like:
  * <pre>
  *
 |-- analyses: struct (nullable = true)
 |    |-- sentences: array (nullable = true)
 |    |    |-- element: struct (containsNull = true)
 |    |    |    |-- beginIndex: integer (nullable = false)
 |    |    |    |-- endIndex: integer (nullable = false)
 |    |    |    |-- tokens: array (nullable = true)
 |    |    |    |    |-- element: struct (containsNull = true)
 |    |    |    |    |    |-- token: string (nullable = true)
 |    |    |    |    |    |-- pos: string (nullable = true)
 |    |    |    |    |    |-- lemma: string (nullable = true)
 |    |    |    |    |    |-- beginIndex: integer (nullable = false)
 |    |    |    |    |    |-- endIndex: integer (nullable = false)
 |    |    |    |-- dependencyParse: array (nullable = true)
 |    |    |    |    |-- element: struct (containsNull = true)
 |    |    |    |    |    |-- relation: string (nullable = true)
 |    |    |    |    |    |-- tokenIdx: integer (nullable = false)
 |    |    |    |    |    |-- headTokenIdx: integer (nullable = false)
  </pre>
  *
  */
object NestedAnnotationsExtractor extends StringSlateExtractor[NestedAnnotations] with Serializable {

  def apply(slate: StringSlate) = {

    val spansAndSentences = slate.iterator[Sentence].toList

    val sentenceStructs = spansAndSentences.map {
      case (sentenceSpan, sentence) =>
        val (spansAndTokens) = slate.covered[Token](sentenceSpan)

        val tokens = spansAndTokens.map {
          case (tokenSpan, token) => NestedTokenSchema(token.token, token.pos.orNull, token.lemma.orNull, tokenSpan.begin, tokenSpan.end)
        }

        val tokenSpanToTokenIndex = spansAndTokens.map(_._1).zipWithIndex.toMap

        val dependencyRelations = slate.
          covered[DependencyRelation](sentenceSpan).
          flatMap {
            case (depRelSpan, depRel) =>
              (depRel.dependent, depRel.head) match {
                case (node, head: LeafDependencyNode) =>
                  val nodeIdx = tokenSpanToTokenIndex(node.span)
                  val headIdx = tokenSpanToTokenIndex(head.span)
                  val rel = depRel.relation
                  Some(NestedDependencyRelationSchema(rel, nodeIdx, headIdx))
                case (node, head: RootDependencyNode) =>
                  val nodeIdx = tokenSpanToTokenIndex(node.span)
                  val headIdx = -1
                  val rel = depRel.relation
                  Some(NestedDependencyRelationSchema(rel, nodeIdx, headIdx))
                case _ => None
              }
          }

        NestedSentenceSchema(sentenceSpan.begin, sentenceSpan.end, tokens, dependencyRelations)
    }
    NestedAnnotations(sentences = sentenceStructs)
  }
}
