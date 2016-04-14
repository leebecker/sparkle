package org.sparkle.typesystem.syntax.dependency

import epic.slab.Sentence
import org.sparkle.slate.Span
import org.sparkle.typesystem.basic.{Token}

import scala.collection.mutable
import scala.collection.JavaConverters._
import collection.JavaConversions._



/**
  * Created by leebecker on 1/8/16.
  */

case class SparkleToken(word: Option[String])

case class SparkleSentence(token: Seq[SparkleToken])

case class SparkleDocument(sentence: Seq[SparkleSentence])

object Quick {
  def convertSparkleSentence(sentenceTokens: java.util.List[java.util.List[String]]) = {
    new SparkleDocument(
      sentence=sentenceTokens.seq.map(tokens =>
        new SparkleSentence(token=tokens.seq.map(token => new SparkleToken(word=Some(token))))
      )
    )
  }



}




trait DependencyNode {
  val token: Option[Token]
  val headRelations: mutable.MutableList[DependencyRelation] = mutable.MutableList[DependencyRelation]()
  val childRelations: mutable.MutableList[DependencyRelation] = mutable.MutableList[DependencyRelation]()
}

case class DependencyRelation(relation: String, node: DependencyNode, head: DependencyNode)

case class TokenDependencyNode(token: Option[Token]=None, tokenSpan: Option[Span]=None) extends DependencyNode {

}

case class RootDependencyNode(sentence: Sentence) extends DependencyNode {
  override val token: Option[Token] = None
}

object DependencyUtils {
  def linkDependencyNodes(relation: String, node: DependencyNode, head: DependencyNode): DependencyRelation = {
    val dep = DependencyRelation(relation, node, head)
    node.headRelations += dep
    head.childRelations += dep
    dep
  }

  def extractToken(node: DependencyNode) = {
    node match {
      case tokenNode: RootDependencyNode => "ROOT"
      case _ => node.token.get.token
    }
  }

  def extractRelation(node: DependencyNode) = {
    if (node.headRelations.isEmpty) {
      ""
    } else {
      node.headRelations.head.relation
    }
  }

  def extractHeadNode(node: DependencyNode) = {
    node.headRelations.head

  }

  def extractTriple(node: DependencyNode): (String, String, String) = {
    node match {
      case tokenNode: TokenDependencyNode => extractTriple(tokenNode.headRelations.head)
    }
  }

  def extractTriple(relation: DependencyRelation): (String, String, String) = {
    (relation.relation, extractToken(relation.node), extractToken(relation.head))
  }
}


