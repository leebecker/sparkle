package org.sparkle.textview

/**
  * Created by leebecker on 12/30/15.
  */
class Annotation(_textView: TextView, _start:Int, _end:Int) extends Ordered[Annotation] {

  private var _span: Tuple2[Int, Int] = (_start,_end)
  def textView: TextView = _textView
  def span = _span
  def span_: (value:Tuple2[Int,Int]):Unit = _span = value
  def start: Int = _span._1
  def end: Int = _span._2
  def start_= (value: Int):Unit = _span = (value, _span._2)
  def end_= (value:Int):Unit = _span = (_span._1, value)
  def coveredText(): String = textView.text().substring(start, end)

  def addToIndex(): Unit = { textView.index += this}

  //def coveredText: String = depot.text.substring(start, end)

  def compare(that: Annotation) = {
    // Annotations are sorted in increasing order of their start offset. That is, for any annotations a and b, if
    // a.start < b.start then a < b.
    // Annotations whose start offsets are equal are next sorted by decreasing order of their end offsets.
    // That is, if a.start = b.start and a.end > b.end, then a < b.
    // This causes annotations with larger to be sorted before annotations with smaller spans, which produces an
    // iteration order similar to a preorder tree traversal.
    // Annotations whose start offsets are equal and whose end offsets are equal are sorted based on
    // TypePriorities (which is an element of the component descriptor). That is, if a.start = b.start, a.end = b.end,
    // and the type of a is defined before the type of b in the type priorities, then a < b.
    // If none of the above rules apply, then the ordering is arbitrary.
    // This will occur if you have two annotations of the exact same type that also have the same span.
    // It will also occur if you have not defined any type priority between two annotations that have the same span.
    val startDiff = this.start - that.start
    if (startDiff == 0) {
      val endDiff = that.end - this.end
      if (endDiff == 0) this.hashCode() - that.hashCode()
      else endDiff
    }
    else startDiff
  }

  override def toString(): String = {
    super.toString() + "(%d, %d)".format(this.start, this.end)
  }




}


object Annotation {
  def create(textView: TextView, start:Int, end:Int) = new Annotation(textView, start, end)
}
