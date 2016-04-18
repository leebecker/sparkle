package org.sparkle.slate

//import epic.preprocess.{RegexSentenceSegmenter, Tokenizer}
//import epic.trees.Span

/**
  * An analysis function that takes a Slate with declared annotation types in it and outputs
  * a new Slate with additional annotations of a new type.
  *
  *   B = Base annotation type
  *   I = Input annotation type
  *   O = Output annotation type
  */


/**
  * Documentation for the type variables:
  *   C = Content type (e.g. String, Image)
  *   R = Region Type
  * @tparam C content type (e.g. String, Image)
  * @tparam R region type (e.g. Span, Block, etc...)
  */
trait AnalysisFunction[C,R] {
  def apply(slate: Slate[C,R]):Slate[C,R]

  def andThen(other: AnalysisFunction[C, R]): AnalysisFunction[C, R] = {
    new ComposedAnalysisFunction[C, R](this, other)
  }

}

case class ComposedAnalysisFunction[C, R](a: AnalysisFunction[C,R], b: AnalysisFunction[C,R])
  extends AnalysisFunction[C, R] {

  def apply(slate: Slate[C,R]):Slate[C,R] = {
    b(a(slate))
  }

}


object StringIdentityAnalyzer extends StringAnalysisFunction {
  def apply(slate: StringSlate):StringSlate = slate
}


