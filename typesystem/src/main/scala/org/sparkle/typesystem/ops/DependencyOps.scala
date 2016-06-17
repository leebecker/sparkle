package org.sparkle.typesystem.ops

import org.sparkle.slate.{Span, StringSlate}

/**
  * Created by leebecker on 6/15/16.
  */
trait DependencyOps[TOKEN_TYPE, NODE_TYPE, LEAF_NODE_TYPE <: NODE_TYPE, ROOT_NODE_TYPE <: NODE_TYPE, RELATION_TYPE] {

  def selectRootNode[In <: NODE_TYPE](slate: StringSlate, coveringSpan: Span): (Span, ROOT_NODE_TYPE)

  def selectLeafNodes[In <: NODE_TYPE](slate: StringSlate, coveringSpan: Span): TraversableOnce[(Span, LEAF_NODE_TYPE)]

  def selectNodes[In <: NODE_TYPE](slate: StringSlate, coveringSpan: Span): TraversableOnce[(Span, NODE_TYPE)]

  def getHeadRelations[In <: RELATION_TYPE with NODE_TYPE](slate: StringSlate, node: NODE_TYPE): Seq[RELATION_TYPE]

  def getHead[In <: RELATION_TYPE](slate: StringSlate, relation: RELATION_TYPE): NODE_TYPE

  def getLabel[In <: RELATION_TYPE](slate: StringSlate, relation: RELATION_TYPE): String

  def createNode(span: Span, token: TOKEN_TYPE): NODE_TYPE

  def createRootNode(span: Span): ROOT_NODE_TYPE

  def createRelation(dependent: NODE_TYPE, head: NODE_TYPE, relation: String): RELATION_TYPE

  def linkNodes(dependent: NODE_TYPE, head: NODE_TYPE, relation: RELATION_TYPE): Unit

  def setHeadRelations(node: NODE_TYPE, headRelations: TraversableOnce[RELATION_TYPE])

  def setChildRelations(node: NODE_TYPE, childRelations: TraversableOnce[RELATION_TYPE])

  def addNodes(slate: StringSlate, nodes: TraversableOnce[NODE_TYPE]): StringSlate

  def addRelations(slate: StringSlate, relations: TraversableOnce[RELATION_TYPE]): StringSlate

}
