package org.sparkle.typesystem.syntax.dependency

import org.sparkle.slate.Span
import org.sparkle.typesystem.basic.{Sentence, Token}

import scala.collection.mutable
import scala.collection.JavaConverters._
import collection.JavaConversions._


abstract class DependencyNode {
  val span: Span
  val token: Option[Token]
  val headRelations: mutable.MutableList[DependencyRelation] = mutable.MutableList()
  val childRelations: mutable.MutableList[DependencyRelation] = mutable.MutableList()
}

case class LeafDependencyNode(span: Span, token: Option[Token]=None) extends DependencyNode

case class RootDependencyNode(span: Span, sentence: Option[Sentence]) extends DependencyNode {
  override val token: Option[Token] = None
}

case class DependencyRelation(relation: String, dependent: DependencyNode, head: DependencyNode) {
  require(dependent != null, "Dependency Relation can not have null dependent node")
  require(head != null, "Dependency Relation can not have null head node")

  def span = {
    val nodeSpan = dependent.span
    val headSpan = head.span
    val begin = if (nodeSpan.begin < headSpan.begin) nodeSpan.begin else headSpan.begin
    val end = if (nodeSpan.end > headSpan.end) nodeSpan.end else headSpan.end
    Span(begin, end)
  }

}


object DependencyUtils {
  def linkDependencyNodes(relation: String, node: DependencyNode, head: DependencyNode): DependencyRelation = {
    val dep = DependencyRelation(relation, node, head)
    node.headRelations += dep
    head.childRelations += dep
    dep
  }


  def filterToLeafNodes(nodes: TraversableOnce[DependencyNode]) = {
    nodes.flatten{
      case (node: RootDependencyNode) => None
      case (node) => Some(node)
    }


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

  def extractTriple(node: DependencyNode): Option[(String, String, String)] = {
    node match {
      case leafNode: LeafDependencyNode => extractTriple(leafNode.headRelations.head)
      case _ => None
    }
  }

  def extractTriple(relation: DependencyRelation): Option[(String, String, String)] = {
    Some((relation.relation, extractToken(relation.dependent), extractToken(relation.head)))
  }
}


