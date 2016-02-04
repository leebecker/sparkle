package org.sparkle.preprocess
import epic.slab.{StringAnalysisFunction, Sentence, Slab}
import org.sparkle.typesystem.basic.{Token}

/**
  * Created by leebecker on 2/4/16.
  *
  *
  */
trait SparkleSentenceSegmenterAndTokenizer extends StringAnalysisFunction[Any, Sentence with Token] with (String => Iterable[String]) with Serializable {
  override def toString = getClass.getName

  def apply(a: String):IndexedSeq[String] = {
    val slab = Slab(a)
    apply(slab).iterator[Sentence with Token].toIndexedSeq.map(s => slab.spanned(s._1))
  }
}
