package org.sparkle.preprocess

import java.util.regex.Pattern

import epic.trees.Span
import epic.slab._
import org.sparkle.typesystem.basic.Token

import scala.collection.mutable.ArrayBuffer

/**
  * Splits the input document according to the given pattern.  Does not
  * return the splits.  Nearly identical to epic.preprocess.Tokenizer, except that this one
  * uses the Sparkle token type
  *
  * @author leebecker
  */
case class RegexSplitTokenizer(pattern : String) extends SparkleTokenizer {

  private val regex = Pattern.compile(pattern)

  def apply[In](slab: StringSlab[In]): StringSlab[In with Token] = {
    val m = regex.matcher(slab.content)

    val spans = new ArrayBuffer[(Span, Token)]()

    var start = 0
    while (m.find()) {
      val end = m.start()
      if (end - start >= 1)
        spans += (Span(start, end) -> Token(slab.content.substring(start, end)))
      start = m.end()
    }
    if(start != slab.content.length)
      spans += Span(start, slab.content.length) -> Token(slab.content.substring(start, slab.content.length))
    slab.addLayer[Token](spans)
  }

}
