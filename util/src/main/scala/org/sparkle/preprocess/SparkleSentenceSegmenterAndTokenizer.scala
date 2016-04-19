package org.sparkle.preprocess
import org.sparkle.slate._

/**
  * Created by leebecker on 2/4/16.
  *
  *
  */

trait SparkleSentenceSegmenterAndTokenizer[SENTENCE, TOKEN] extends StringAnalysisFunction with Serializable {
  override def toString = getClass.getName

  def apply(a: String):IndexedSeq[String] = {
    val slate = Slate(a)
    apply(slate).iterator[SENTENCE with TOKEN].toIndexedSeq.map(s => slate.spanned(s._1))
  }
}

