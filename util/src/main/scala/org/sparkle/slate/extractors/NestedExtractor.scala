package org.sparkle.slate.extractors

import org.sparkle.slate._
import org.sparkle.slate.StringSlateExtractor
import org.sparkle.typesystem.basic.{Sentence, Token}
import org.sparkle.typesystem.syntax.dependency._


// By convention -1 is the top node
case class DependencyRelationSchema(relation:String, tokenIdx: Int, headTokenIdx: Int)

case class TokenSchema(token: String, pos: String, lemma: String, beginIndex: Int, endIndex: Int)

case class SentenceSchema(beginIndex: Int, endIndex: Int, tokens: Seq[TokenSchema], dependencyParse: Seq[DependencyRelationSchema])

case class NestedExtractorAnalyses(sentences: Seq[SentenceSchema])


/**
  *
  */
object NestedExtractor extends StringSlateExtractor[NestedExtractorAnalyses] with Serializable {

  def apply(slate: StringSlate) = {

    val spansAndSentences = slate.iterator[Sentence].toList

    val sentenceStructs = spansAndSentences.map {
      case (sentenceSpan, sentence) => {
        val (spansAndTokens) = slate.covered[Token](sentenceSpan)

        val tokens = spansAndTokens.map {
          case (tokenSpan, token) => TokenSchema(token.token, token.pos.orNull, token.lemma.orNull, tokenSpan.begin, tokenSpan.end)
        }

        val tokenSpanToTokenIndex = spansAndTokens.map(_._1).zipWithIndex.toMap

        val dependencyRelations = slate.
          covered[DependencyRelation](sentenceSpan).
          flatMap {
            case (depRelSpan, depRel) => {
              (depRel.dependent, depRel.head) match {
                case (node, head: LeafDependencyNode) =>
                  val nodeIdx = tokenSpanToTokenIndex(node.span)
                  val headIdx = tokenSpanToTokenIndex(head.span)
                  val rel = depRel.relation
                  Some(DependencyRelationSchema(rel, nodeIdx, headIdx))
                case (node, head: RootDependencyNode) =>
                  val nodeIdx = tokenSpanToTokenIndex(node.span)
                  val headIdx = -1
                  val rel = depRel.relation
                  Some(DependencyRelationSchema(rel, nodeIdx, headIdx))
                case _ => None
              }
            }
          }



        val x = spansAndTokens.zipWithIndex.flatMap {
          case ((tokenSpan, token), tokenIdx) => {
            val node = slate.covered[DependencyNode](tokenSpan).headOption
            if (node.isDefined) {

            }
            node
          }
        }

        tokens.zipWithIndex.map {
          t=> t
          //case ((tokenSpan, token), tokenIdx) => slate.covered[DependencyNode]

        }

        SentenceSchema(sentenceSpan.begin, sentenceSpan.end, tokens, dependencyRelations)
      }
    }
    NestedExtractorAnalyses(sentences = sentenceStructs)
  }
}
