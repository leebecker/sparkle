package org.sparkle.typesystem

import org.sparkle.slab.{SlabAnnotationOps, Slab}

import scala.reflect.ClassTag

/**
  * Created by leebecker on 1/7/16.
  */
// =========================
// Annotation infrastructure
// =========================
trait Span {
  val begin: Int
  val end: Int
}

object Span {
  implicit class SpanInStringSlab(val span: Span) extends AnyVal {
    def in[AnnotationTypes <: Span](slab: Slab.StringSlab[AnnotationTypes]) =
      new StringSpanAnnotationOps(this.span, slab)
  }

  class StringSpanAnnotationOps[AnnotationType >: AnnotationTypes <: Span: ClassTag, AnnotationTypes <: Span](
    annotation: AnnotationType,
    slab: Slab.StringSlab[AnnotationTypes])
    extends SlabAnnotationOps[String, Span, AnnotationType, AnnotationTypes](annotation, slab) {
    def content = this.slab.content.substring(this.annotation.begin, this.annotation.end)
  }

  implicit object StringAnnotationHasBounds extends Slab.HasBounds[Span] {
    def covers(annotation1: Span, annotation2: Span): Boolean =
      annotation1.begin <= annotation2.begin && annotation2.end <= annotation1.end
    def follows(annotation1: Span, annotation2: Span): Boolean =
      annotation2.end <= annotation1.begin
    def precedes(annotation1: Span, annotation2: Span): Boolean =
      annotation1.end <= annotation2.begin
  }
}

