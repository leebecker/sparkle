package org.sparkle.slate

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



