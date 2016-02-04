package org.sparkle.typesystem.syntax.dependency

import epic.trees.Span
import org.sparkle.typesystem.basic.{Sentence, Token}

import scala.collection.mutable


/**
  * Created by leebecker on 1/8/16.
  */

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


