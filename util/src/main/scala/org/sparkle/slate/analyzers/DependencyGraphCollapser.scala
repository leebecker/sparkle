package org.sparkle.slate.analyzers

import org.sparkle.slate.{Slate, Span, StringAnalysisFunction, StringSlate}
import org.sparkle.typesystem.syntax.dependency._

import scala.collection.mutable
import scala.reflect.ClassTag

/**
  * Analyzer which accepts a Slate annotated with Dependency Nodes and Relations and outputs a slate
  * with a layer for Collapsed Dependency Nodes and Relations
  *
  * @tparam WINDOW - Type of annotation to extract dependency graph from.  Usually this is a Sentence.
  */
class DependencyGraphCollapser[WINDOW:ClassTag] extends StringAnalysisFunction with Serializable {
  override def apply(slate: Slate[String, Span]): Slate[String, Span] = {

    // Iterate over all windows
    val graphs = slate.indexedSeq[WINDOW].map{
      case (windowSpan, window) =>
        val nodes = slate.covered[DependencyNode](windowSpan)
        val (collapsedRels, collapsedNodes) = collapseGraph(nodes)
        (collapsedRels.map(r=>(r.span, r)), collapsedNodes.map(n=>(n.span, n)))
    }.unzip

    val (collapsedRels, collapsedNodes) = (graphs._1.flatten, graphs._2.flatten)

    // Add annotations and return slate
    slate.
      addLayer[CollapsedDependencyRelation](collapsedRels).
      addLayer[CollapsedDependencyNode](collapsedNodes)
  }


  /**
    * Given a set of nodes representing a dependency graph, returns a collapsed dependency graph
    * @param nodeAnnotations nodes representing the dependency graph
    * @return pair tuple containing nodes and relations for the collapsed graph
    */
  def collapseGraph(nodeAnnotations: IndexedSeq[(Span, DependencyNode)]): (Seq[CollapsedDependencyRelation], Seq[CollapsedDependencyNode]) = {

    val spansToNodes = nodeAnnotations.toMap
    var spansToCollapsedNodes = mutable.Map[Span, CollapsedDependencyNode]()

    val collapsedRelations = nodeAnnotations.flatMap {
      case (span, node) =>
        node.headRelations.flatMap {
          // create relations for all head relations for a given node
          relation => {

            // Retrieve collapsed head from span to node lookup or create one if necessary
            // the lookup gets update later so that unlinked nodes do not make it into the final graph
            val collapsedHead = relation.head match {
              case (head: RootDependencyNode) =>
                spansToCollapsedNodes.getOrElse(head.span, RootCollapsedDependencyNode(head.span, None))
              case (head: LeafDependencyNode) =>
                spansToCollapsedNodes.getOrElse(head.span, LeafCollapsedDependencyNode(head.span, head.token))
            }

            if (relation.relation == "prep") {
              // Prepositions should be collapsed with transitivity, by removing the node and linking its head with its children.
              node.childRelations.map {
                childRelation => {
                  // for prepositions the relation label incorporates the deleted nodes token
                  val collapsedRelationLabel = relation.relation + "_" + node.token.get.token.toLowerCase()
                  val collapsedNode = spansToCollapsedNodes.getOrElseUpdate(childRelation.dependent.span,
                    LeafCollapsedDependencyNode(childRelation.dependent.span, childRelation.dependent.token))
                  val collapsedRelation = CollapsedDependencyRelation(collapsedRelationLabel, collapsedNode, collapsedHead)
                  linkNodes(collapsedNode, collapsedHead, collapsedRelation)
                  spansToCollapsedNodes.put(collapsedHead.span, collapsedHead)
                  collapsedRelation
                }
              }.toList
            } else if (relation.relation == "pobj") {
              // Preposition object relations should be skipped as they get collapsed with prepositions
              List[CollapsedDependencyRelation]()
            } else {
              // Default, directly convert relation into collapsed relation
              val collapsedNode = spansToCollapsedNodes.getOrElseUpdate(node.span, LeafCollapsedDependencyNode(node.span, node.token))
              val collapsedRelation = CollapsedDependencyRelation(relation.relation, collapsedNode, collapsedHead)
              linkNodes(collapsedNode, collapsedHead, collapsedRelation)
              spansToCollapsedNodes.put(collapsedHead.span, collapsedHead)
              List(collapsedRelation)
            }
          }
        }
    }

    (collapsedRelations, spansToCollapsedNodes.values.toSeq)
  }

  def linkNodes(dependent: CollapsedDependencyNode, head: CollapsedDependencyNode, relation: CollapsedDependencyRelation): Unit = {
    dependent.headRelations += relation
    head.childRelations += relation
  }

  def extractContext(slate: StringSlate, targetWord: Span) = {

    slate.covered[CollapsedDependencyNode](targetWord).flatMap {
      case(targetSpan, targetNode) =>
        val headContext = targetNode.headRelations.map{
          rel => rel.head match {
            case (head: RootCollapsedDependencyNode) => s"_TOP_/${rel.relation}-1"
            case (head: LeafCollapsedDependencyNode) =>
              if (head.token.isEmpty) {
                "_NONE_/" + rel.relation + "-1"
              } else {
                val headToken = head.token.get.token.toLowerCase()
                s"$headToken/${rel.relation}-1"
              }
          }
        }

        val childContext = targetNode.childRelations.map {
           rel =>
              if (rel.dependent.token.isEmpty) {
                s"_NONE_/${rel.relation}"
              } else {
                val childToken = rel.dependent.token.get.token.toLowerCase()
                s"$childToken/${rel.relation}"
              }
        }
        headContext ++ childContext
    }
  }
}
