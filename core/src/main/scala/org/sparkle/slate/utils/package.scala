package org.sparkle.slate

import scala.collection.TraversableOnce
import scala.reflect.ClassTag

/**
  * Created by leebecker on 6/17/16.
  */
package object utils {
  implicit class StringSlateAnnotationSpansIter[A: ClassTag](annotationsIter: TraversableOnce[(Span, A)]) {
    def filterByType[SUB_A: ClassTag] = annotationsIter.flatMap {
      case (span: Span, annotation: SUB_A) => Some(span, annotation)
      case otherwise => None
    }

    def filterByNotType[SUB_A: ClassTag] = annotations.flatMap {
      case (span: Span, annotation: SUB_A) => None
      case otherwise => Some(otherwise)
    }

    def spans = annotationsIter.map{_._1}

    def annotations = annotationsIter.map{_._2}
  }

  implicit class StringSlateAnnotationSpansSeq[A: ClassTag](annotationsSeq: Seq[(Span, A)]) {
    def filterByType[SUB_A: ClassTag] = annotationsSeq.flatMap {
      case (span: Span, annotation: SUB_A) => Some(span, annotation)
      case otherwise => None
    }

    def filterByNotType[SUB_A: ClassTag] = annotationsSeq.flatMap {
      case (span: Span, annotation: SUB_A) => None
      case otherwise => Some(otherwise)
    }

    def spans = annotationsSeq.map{_._1}

    def annotations = annotationsSeq.map{_._2}
  }
}
