package org.sparkle.preprocess

import org.sparkle.slate._
import org.sparkle.typesystem.basic.Sentence
/**
  * Created by leebecker on 4/18/16.
  */

trait SparkleSentenceSegmenter extends StringAnalysisFunction with Serializable {
  override def toString = getClass.getName

  def apply(a: String):IndexedSeq[String] = {
    val slate = Slate(a)
    apply(slate).iterator[Sentence].toIndexedSeq.map(s => slate.spanned(s._1))
  }

}
