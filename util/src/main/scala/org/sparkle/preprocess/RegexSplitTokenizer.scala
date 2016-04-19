package org.sparkle.preprocess

import java.util.regex.Pattern

import org.sparkle.slate._
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

  def apply(slate: StringSlate): StringSlate = {
    val m = regex.matcher(slate.content)

    val spans = new ArrayBuffer[(Span, Token)]()

    var start = 0
    while (m.find()) {
      val end = m.start()
      if (end - start >= 1)
        spans += (Span(start, end) -> Token(slate.content.substring(start, end)))
      start = m.end()
    }
    if(start != slate.content.length)
      spans += Span(start, slate.content.length) -> Token(slate.content.substring(start, slate.content.length))
    slate.addLayer[Token](spans)
  }

}
