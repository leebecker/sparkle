package org.sparkle.typesystem.syntax.dependency

import org.sparkle.slate.Span
import org.sparkle.typesystem.basic.{Sentence, Token}

import scala.collection.mutable
import scala.collection.JavaConverters._
import collection.JavaConversions._


abstract class CollapsedDependencyNode {
  val span: Span
  val token: Option[Token]
  val headRelations: mutable.MutableList[CollapsedDependencyRelation] = mutable.MutableList()
  val childRelations: mutable.MutableList[CollapsedDependencyRelation] = mutable.MutableList()

  def nodePath(): Seq[CollapsedDependencyNode]

  override def toString = if (token.isDefined) token.get.token else "_UNDEFINED_"
}

case class LeafCollapsedDependencyNode(span: Span, token: Option[Token]=None) extends CollapsedDependencyNode {

  override def nodePath() = {
    if (headRelations.isEmpty) {
      this::Nil
    } else {
      List(this) ++ headRelations.head.head.nodePath()
    }
  }

}

case class RootCollapsedDependencyNode(span: Span, sentence: Option[Sentence]) extends CollapsedDependencyNode {
  override val token: Option[Token] = None

  override def nodePath() = Nil

  override def toString = "_TOP_"
}

case class CollapsedDependencyRelation(relation: String, dependent: CollapsedDependencyNode, head: CollapsedDependencyNode) {
  require(dependent != null, "Dependency Relation can not have null dependent node")
  require(head != null, "Dependency Relation can not have null head node")

  def span = {
    val nodeSpan = dependent.span
    val headSpan = head.span
    val begin = if (nodeSpan.begin < headSpan.begin) nodeSpan.begin else headSpan.begin
    val end = if (nodeSpan.end > headSpan.end) nodeSpan.end else headSpan.end
    Span(begin, end)
  }

  override def toString = s"$relation($dependent, $head)"


}
