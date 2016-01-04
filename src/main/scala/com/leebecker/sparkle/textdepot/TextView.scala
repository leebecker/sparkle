package com.leebecker.sparkle.textdepot

/**
 * Created by leebecker on 12/30/15.
 */

import com.leebecker.sparkle.typesystem.Annotation

import scala.collection.mutable
import scala.math.Ordering.Implicits._


case class TextViewException(smth:String)  extends Exception


trait TextView {
  def apply(view: String): TextView

  def apply(): TextView = this.apply(TextView.DefaultViewName)

  val views: mutable.HashMap[String, TextView]

  val index: mutable.TreeSet[Annotation]

  @throws(classOf[Exception])
  def text(): String

  @throws(classOf[Exception])
  def text(viewText: String): Unit
}

// Should always traverse back to master
// every one has its own treeset index
class TextViewAnalysisImpl(viewName: String, master: Option[TextView]) extends TextView {
  def this() = this(TextView.DefaultViewName, None)

  val views: mutable.HashMap[String, TextView] = master match {
    case None => mutable.HashMap[String, TextView]()
    case Some(m) => m.views
  }

  val index: mutable.TreeSet[Annotation] = mutable.TreeSet[Annotation]()

  override def apply(view: String) = this.views(view)

  def createView(viewName: String): TextView = new TextViewAnalysisImpl(viewName, Some(this))

  var _text: Option[String] = None

  @throws(classOf[Exception])
  def text(): String = {
    if (_text.isDefined) _text.get
    else throw new TextViewException("Text for TextView is undefined")
  }

  @throws(classOf[Exception])
  override def text(viewText: String): Unit = {
    if (_text.isDefined) {
      throw new TextViewException("Text already set for TextView")
    } else {
      _text = Some(viewText)
    }
  }
}

object TextView {
  val DefaultViewName: String = "_DEFAULT_VIEW"
  //def create(): TextViewAnalysis = new TextViewAnalysisImpl(DefaultViewName)
  //def create(viewName: String): TextViewAnalysis = new TextViewAnalysisImpl(viewName)
}


class TextAnnotationIndex {

  var documentText: String = null
  var index: mutable.TreeSet[Annotation] = mutable.TreeSet[Annotation]()
  var views: mutable.HashMap[String, TextAnnotationIndex] = mutable.HashMap[String, TextAnnotationIndex]()

  def apply(): Unit = {

  }

  def apply(view: String): Unit = {


  }
}

object MinOrder extends Ordering[Annotation] {
     def compare(x:Annotation, y:Annotation) = y compare x
}



