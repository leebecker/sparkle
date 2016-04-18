package org.sparkle.preprocess
import org.sparkle.slate.{StringAnalysisFunction, Slate}
import org.sparkle.typesystem.basic.{Sentence,Token}

/**
  * Created by leebecker on 2/4/16.
  *
  *
  */
trait SparkleSentenceSegmenterAndTokenizer extends StringAnalysisFunction with Serializable {
  override def toString = getClass.getName

  def apply(a: String):IndexedSeq[String] = {
    val slate = Slate(a)
    apply(slate).iterator[Sentence with Token].toIndexedSeq.map(s => slate.spanned(s._1))
  }
}
