package org.sparkle.textview

/**
 * Created by leebecker on 12/30/15.
 */

import scala.collection.mutable

/**
  * Base trait which uses encapsulates the functionality for working with views of texts
  * and annotations on those views.
  */
trait TextView {
  /**
    * Retrieves named view of the TextView
    * @param view
    * @return
    */
  def apply(view: String): TextView

  /**
    * Retrieves the DefaultView for the TextView
    * @return
    */
  def apply(): TextView = this.apply(TextView.DefaultViewName)

  val views: mutable.HashMap[String, TextView]

  val index: mutable.TreeSet[Annotation]

  /**
    * Creates a view on this textView with specified name
    * @param viewName
    * @return
    */
  def createView(viewName: String): TextView

  @throws(classOf[Exception])
  def text: String

  @throws(classOf[Exception])
  def text_= (viewText: String): Unit

  /**
    * Convenience method for iterator over all Annotation in TextView
    * @return
    */
  def iterator(): Iterator[Annotation]

  /**
    * Convenience method for iterator over all Annotations matching specified type
    * @param clazz - class of annotations to match on (e.g. Token, Sentence, etc...)
    * @return
    */
  def iterator(clazz: Class[_ <: Annotation]): Iterator[Annotation]

  /**
    * Get an iterator of all Annotations of specified type between start and end
    * @param clazz
    * @param start
    * @param end
    * @return
    */
  def covered(clazz: Class[_ <: Annotation], start: Int, end: Int): Iterator[Annotation]

  /**
    * Iterator of all [[org.sparkle.textview.Annotation]]s matching type covered by specified annotation
    * @param clazz
    * @param coveringAnnotation
    * @return
    */
  def covered(clazz: Class[_ <: Annotation], coveringAnnotation: Annotation): Iterator[Annotation]
}

/**
  * Class which implements base functionality of the TextView trait
  * @param viewName
  * @param master
  */
class TextViewImpl(viewName: String, master: Option[TextView]) extends TextView {
  def this() = this(TextView.DefaultViewName, None)

  val views: mutable.HashMap[String, TextView] = master match {
    case None => mutable.HashMap[String, TextView](TextView.DefaultViewName -> this)
    case Some(m) => m.views
  }

  val index: mutable.TreeSet[Annotation] = mutable.TreeSet[Annotation]()

  override def apply(view: String) = this.views(view)

  override def createView(viewName: String): TextView = {
    val newView = new TextViewImpl(viewName, Some(this))
    newView.views(viewName) = newView
    newView
  }

  var _text: Option[String] = None

  @throws(classOf[Exception])
  def text(): String = {
    if (_text.isDefined) _text.get
    else throw new TextViewException("Text for TextView is undefined")
  }

  @throws(classOf[Exception])
  override def text_=(viewText: String): Unit = {
    if (_text.isDefined) {
      throw new TextViewException("Text already set for TextView")
    } else {
      _text = Some(viewText)
    }
  }

  override def iterator(): Iterator[Annotation] = this.index.iterator

  override def iterator(clazz: Class[_ <: Annotation]): Iterator[Annotation] =
    iterator.withFilter(x=>clazz.isInstance(x))

  override def covered(clazz: Class[_ <: Annotation], start: Int, end: Int): Iterator[Annotation] =
    iterator(clazz)
      .withFilter(annotation => annotation.start >= start)
      .withFilter(annotation => annotation.end <= end)

  override def covered(clazz: Class[_ <: Annotation], coveringAnnotation: Annotation): Iterator[Annotation] =
    covered(clazz, coveringAnnotation.start, coveringAnnotation.end)

}

/**
  * Companion object for TextView, which primarily contains constants and factories
  * used by [[org.sparkle.textview.TextView]]
  */
object TextView {
  val DefaultViewName: String = "_DEFAULT_VIEW"
  def create(): TextView = new TextViewImpl()
}
