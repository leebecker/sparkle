package org.sparkle.textview

/**
  * Created by leebecker on 1/4/16.
  */
import org.scalatest._

class TextViewSpec extends FlatSpec with Matchers {

  it should "have text after setting" in {
    val tv = TextView.create()
    val someText = "This is some text."
    tv.text = someText
    tv.text should be (someText)
  }

  "A default TextView" should "be its own master" in {
    val tv = TextView.create()
    tv() should be (tv)
  }

  "A sub-view on a TextView" should "be able to get back to the master TextView" in {
    val tv = TextView.create()
    val childView = tv.createView("childView")
    childView() should be (tv)
    childView.views should be (tv.views)
  }

  it should "create a view which is managed by its master" in {
    val tv = TextView.create()
    val childView = tv.createView("childView")
    tv("childView") should be (childView)
  }

  it should "throw TextViewException if text field is written to more than once" in {
    val tv = TextView.create()
    tv.text = "Original Text"
    a[TextViewException] should be thrownBy {
      tv.text = "New Text"
    }
  }

  it should "permit repeated annotations for the same type" in {
    val tv = TextView.create()
    tv.text = "This is a sentence"
    val a1 = Annotation.create(tv, 0, 4)
    a1.addToIndex()
    val a2 = Annotation.create(tv, 0, 4)
    a2.addToIndex()
    println(tv.index)
    tv.select(classOf[Annotation]).size should be (2)
  }

  it should "select by covered type" in {
    val tv = TextView.create()
    class FooAnnotation(textView: TextView, start:Int, end:Int) extends Annotation(textView, start, end)

    val a1 = Annotation.create(tv, 0, 4)
    val a2 = new FooAnnotation(tv, 0, 4)
    a1.addToIndex()
    a2.addToIndex()

    tv.select(classOf[Annotation]).size should be (2)
    tv.select(classOf[FooAnnotation]).size should be (1)
  }
}
