package org.sparkle.typesystem.syntax.dependency

import org.sparkle.typesystem.basic.{Sentence, Token}

import scala.collection.mutable


/**
  * Created by leebecker on 1/8/16.
  */

trait DependencyNode {
  val headRelations: mutable.MutableList[DependencyRelation] = mutable.MutableList[DependencyRelation]()
  val childRelations: mutable.MutableList[DependencyRelation] = mutable.MutableList[DependencyRelation]()
}

case class DependencyRelation(relation: String, node: DependencyNode, head: DependencyNode)

case class TokenDependencyNode(token: Option[Token]=None) extends DependencyNode

case class RootDependencyNode(sentence: Sentence) extends DependencyNode

object DependencyUtils {
  def linkDependencyNodes(relation: String, node: DependencyNode, head: DependencyNode): DependencyRelation = {
    val dep = DependencyRelation(relation, node, head)
    node.headRelations += dep
    head.childRelations += dep
    dep
  }
}


