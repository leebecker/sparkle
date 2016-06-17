package org.sparkle.slate

import scala.collection.TraversableOnce
import scala.reflect.ClassTag

/**
  * Created by leebecker on 6/17/16.
  */
package object utils {
  implicit class StringSlateAnnotationSpans[A: ClassTag](annotations: TraversableOnce[(Span, A)]) {
    def filterByType[SUB_A: ClassTag] = annotations.flatMap {
      case (span: Span, annotation: SUB_A) => Some(span, annotation)
      case otherwise => None
    }
  }
}
