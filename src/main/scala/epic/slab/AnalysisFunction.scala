package epic.slab

import epic.trees.Span

/**
  * Adapted from scala-nlp's epic and chalk projects
  */

/**
  * An analysis function that takes a Slab with declared annotation types in it and outputs
  * a new Slab with additional annotations of a new type.
  *
  * Documentation for the type variables:
  *   C = Content type
  *   B = Base annonation type
  *   I = Input annotation type
  *   O = Output annotation type
 **/
trait AnalysisFunction[C,B,I,+O] {
  def apply[In <: I](slab: Slab[C,B,In]):Slab[C,B,In with O]

  def andThen[II >: (I with O), OO](other: AnalysisFunction[C, B, II, OO]):AnalysisFunction[C, B, I, O with OO] = {
    new ComposedAnalysisFunction[C, B, I, O, II, OO](this, other)
  }

}

case class ComposedAnalysisFunction[C, B, I, O, II >: (I with O), +OO](a: AnalysisFunction[C,B,I,O],
                                                                       b: AnalysisFunction[C,B,II,OO]) extends AnalysisFunction[C, B, I, O with OO] {

  def apply[In <: I](slab: Slab[C,B,In]):Slab[C,B,In with O with OO] = {
    // type inference can't figure this out...
    // can you blame it?
    b[In with O](a[In](slab))
  }

}


object StringIdentityAnalyzer extends StringAnalysisFunction[Any, Any] {
  def apply[In](slab: StringSlab[In]):StringSlab[In] = slab
}

object AnalysisPipeline {
  import AnnotatedSpan._

  // added only to demonstrate necessity of [I] parameter on analyzers
  private[AnalysisPipeline] case class Document()
  private[AnalysisPipeline] def documentAdder(slab: StringSlab[Any]) =
    slab.addLayer(Iterator(Span(0, slab.content.length) -> Document()))

  /*
  def main (args: Array[String]) {
    def sentenceSegmenter:StringAnalysisFunction[Any, Sentence] = RegexSentenceSegmenter
    def tokenizer = RegexTokenizer
    val pipeline = sentenceSegmenter andThen tokenizer

    val inSlab = Slab("test\n.").+(Span(0, 5) -> Document())
    val slab = pipeline(inSlab)

    // just to show what the type is
    val typedSpan: Slab[String, Span, Document with Sentence with Token] = slab

    // added only to demonstrate necesssity of [I] parameter on analyzers
    val paragraphs = slab.iterator[Document].toList


    val sentences = slab.iterator[Sentence].toList
    println("\nSENTENCES\n\n" + sentences.map(r => slab.spanned(r._1)).mkString("\n\n"))
    
    val tokens = slab.iterator[Token].toList
    println("\nTOKENS\n\n" + tokens.map(r => slab.spanned(r._1)).mkString("\n\n"))

  }
  */


}
