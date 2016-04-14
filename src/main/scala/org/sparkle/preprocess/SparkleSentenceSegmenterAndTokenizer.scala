package org.sparkle.preprocess
import org.sparkle.slate.{StringAnalysisFunction, Slate}
import epic.slab.Sentence
import org.sparkle.typesystem.basic.{Token}

/**
  * Created by leebecker on 2/4/16.
  *
  *
  */
trait SparkleSentenceSegmenterAndTokenizer extends StringAnalysisFunction with Serializable {
  override def toString = getClass.getName

  def apply(a: String):IndexedSeq[String] = {
    val slab = Slate(a)
    apply(slab).iterator[Sentence with Token].toIndexedSeq.map(s => slab.spanned(s._1))
  }
}
