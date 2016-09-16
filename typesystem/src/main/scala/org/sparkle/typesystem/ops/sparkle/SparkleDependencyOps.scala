package org.sparkle.typesystem.ops.sparkle

import org.sparkle.slate.{Span, StringSlate}
import org.sparkle.typesystem.basic._
import org.sparkle.typesystem.ops.DependencyOps
import org.sparkle.typesystem.syntax.dependency._

import scala.collection.TraversableOnce

/**
  * Created by leebecker on 6/16/16.
  */
object SparkleDependencyOps extends DependencyOps[Token, DependencyNode, LeafDependencyNode, RootDependencyNode, DependencyRelation]{

  override def selectNodes[In <: DependencyNode](slate: StringSlate, coveringSpan: Span): TraversableOnce[(Span, DependencyNode)] =
    slate.covered[DependencyNode](coveringSpan)

  override def selectLeafNodes[In <: DependencyNode](slate: StringSlate, coveringSpan: Span): TraversableOnce[(Span, LeafDependencyNode)] = {
    slate.covered[DependencyNode](coveringSpan).flatten {
      case (span, node: LeafDependencyNode) => Option(span, node)
      case _ => None
    }
  }

  override def selectRootNode[In <: DependencyNode](slate: StringSlate, coveringSpan: Span): (Span, RootDependencyNode) = {
    val rootNodes = slate.covered[DependencyNode](coveringSpan).flatten{
        case (span, node: RootDependencyNode) => Option(span, node)
        case _ => None
    }
    require(rootNodes.nonEmpty, "No root nodes")
    rootNodes.head
  }

  override def createNode(span: Span, token: Token): DependencyNode = LeafDependencyNode(span, Some(token))

  override def createRootNode(span: Span): RootDependencyNode = RootDependencyNode(span, None)


  override def setChildRelations(node: DependencyNode, childRelations: TraversableOnce[DependencyRelation]): Unit =
    childRelations.foreach(rel => node.childRelations += rel)

  override def setHeadRelations(node: DependencyNode, headRelations: TraversableOnce[DependencyRelation]): Unit =
    headRelations.foreach(rel => node.childRelations += rel)

  override def getLabel[In <: DependencyRelation](slate: StringSlate, relation: DependencyRelation): String = relation.relation

  override def addNodes(slate: StringSlate, nodes: TraversableOnce[DependencyNode]): StringSlate = {
    val nodeSpans = nodes.map(node=>(node.span, node))
    slate.addLayer[DependencyNode](nodeSpans)
  }

  override def addRelations(slate: StringSlate, relations: TraversableOnce[DependencyRelation]): StringSlate = {
    val relSpans = relations.map(relation=>(relation.span, relation))
    slate.addLayer[DependencyRelation](relSpans)
  }

  override def getHead[In <: DependencyRelation](slate: StringSlate, relation: DependencyRelation): DependencyNode = relation.head

  override def getHeadRelations[In <: DependencyRelation with DependencyNode](slate: StringSlate, node: DependencyNode): Seq[DependencyRelation] =
    node.headRelations.toSeq


  override def createRelation(dependent: DependencyNode, head: DependencyNode, relation: String): DependencyRelation = DependencyRelation(relation, dependent, head)

  override def linkNodes(dependent: DependencyNode, head: DependencyNode, relation: DependencyRelation): Unit = {
    dependent.headRelations += relation
    head.childRelations += relation
  }

}
