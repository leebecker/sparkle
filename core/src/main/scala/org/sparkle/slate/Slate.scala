package org.sparkle.slate

import org.sparkle.util.BinarySearch

import scala.reflect.ClassTag
import org.sparkle.slate.AnnotatedSpan.{EndFirstSpanOrdering, SpanOrdering}

/**
  * Blatantly modified (err... stolen) from Epic's Slabs.  This removed the notion of Input and Output annotation types
  * to allow for more flexibility in passing pipelines and extractors to Spark methods.
  *
  * A Slate is a central data structure for annotating content by regions.  Most typically, this will be a
  * Text with Spans.
  */
trait Slate[ContentType, RegionType] extends Serializable {

  val content: ContentType

  def spanned(region: RegionType): ContentType

  // Add a new annotation to the slate
  def append[A:ClassTag](region: RegionType, annotation: A): Slate[ContentType, RegionType] = {
    this.+[A](region -> annotation)
  }

  // Add a new annotation to the slate
  def +[A:ClassTag](pair: (RegionType, A)): Slate[ContentType, RegionType] = {
    addLayer[A](pair)
  }

  def addLayer[A:ClassTag](annotations: TraversableOnce[(RegionType, A)]): Slate[ContentType, RegionType]

  def addLayer[A:ClassTag](annotations: (RegionType, A)*): Slate[ContentType, RegionType] = {
    addLayer[A](annotations)
  }

  /** Can't remove the type, but you can upcast */
  def removeLayer[A: ClassTag]: Slate[ContentType, RegionType]

  /** useful for downcasting */
  def checkedCast[A: ClassTag]:Option[Slate[ContentType, RegionType]] = {
    if(!hasLayer[A]) {
      None
    } else {
      Some(this.asInstanceOf[Slate[ContentType, RegionType]])
    }
  }

  /** Queries whether we have annotations of this type, even if the slate
    *  doesn't have this type. Sometimes you just have to cast... */
  def hasLayer[A :ClassTag]:Boolean

  def iterator[A : ClassTag]: Iterator[(RegionType, A)]

  def indexedSeq[A : ClassTag]: IndexedSeq[(RegionType, A)]

  /**
    * Returns annotations wholly contained in the region
    *
    * @param region
    * @tparam A
    * @return
    */
  def covered[A : ClassTag](region: RegionType): IndexedSeq[(RegionType, A)]


  /**
    * Returns annotations that are entirely before the region
    *
    * @param region
    * @tparam A
    * @return
    */
  def preceding[A : ClassTag](region: RegionType): Iterator[(RegionType, A)]

  def following[A : ClassTag](region: RegionType): Iterator[(RegionType, A)]

  /*
  def stringRep[A >: AnnotationTypes: ClassTag](annotationType: ClassTag[A]) = {
    iterator[A].mkString("\n")
  }
  */

}

object AnnotatedSpan {

  implicit object SpanOrdering extends Ordering[Span] {
    override def compare(x: Span, y: Span): Int = {
      if      (x.begin < y.begin) -1
      else if (x.begin > y.begin)  1
      else if (x.end  < y.end)    -1
      else if (x.end > y.end)      1
      else                         0
    }
  }

  implicit object EndFirstSpanOrdering extends Ordering[Span] {
    override def compare(x: Span, y: Span): Int = {
      if (x.end  < y.end)    -1
      else if (x.end > y.end)      1
      else if (x.begin < y.begin) -1
      else if (x.begin > y.begin)  1
      else                         0
    }
  }

}


object Slate {

  trait ExtractRegion[Region, T] extends Serializable {
    def apply(region: Region, t: T):T
  }

  implicit object SpanStringExtractRegion extends ExtractRegion[Span, String] {
    def apply(region: Span, t: String) = t.substring(region.begin, region.end)
  }

  def apply(content: String):StringSlate = {
    new SortedSequenceSlate(content, Map.empty, Map.empty)
  }

  /**
    * This slate should be more efficient, especially for longer documents. It maintains the annotations in sorted order.
    *
    * @param content
    * @param annotations
    * @tparam ContentType
    */
  private[slate] class SortedSequenceSlate[ContentType]
  (val content: ContentType,
    val annotations: Map[Class[_], Vector[(Span, Any)]] = Map.empty,
    val reverseAnnotations: Map[Class[_], Vector[(Span, Any)]] = Map.empty)(implicit extract: ExtractRegion[Span, ContentType]) extends Slate[ContentType, Span] {

    override def spanned(region: Span): ContentType = extract(region, content)

    override def addLayer[A:ClassTag](annotations: TraversableOnce[(Span, A)]): Slate[ContentType, Span] = {
      val ann = annotations.toSeq
      var newAnnotations = this.annotations
      val clss = implicitly[ClassTag[A]].runtimeClass
      newAnnotations = newAnnotations + (clss -> (newAnnotations.getOrElse(clss, Vector.empty) ++ ann).sortBy(_._1)(SpanOrdering))

      val reverseAnnotations = {
        this.reverseAnnotations + (clss -> (this.reverseAnnotations.getOrElse(clss, Vector.empty) ++ ann).sortBy(_._1)(EndFirstSpanOrdering))
      }

      new SortedSequenceSlate(content, newAnnotations, reverseAnnotations)
    }


    override def removeLayer[A :  ClassTag]: Slate[ContentType, Span] = {
      new SortedSequenceSlate(content,
        annotations - implicitly[ClassTag[A]].runtimeClass,
        reverseAnnotations - implicitly[ClassTag[A]].runtimeClass)
    }


    /** Queries whether we have annotations of this type, even if the slate
      * doesn't have this type. Sometimes you just have to cast... */
    override def hasLayer[A: ClassTag]: Boolean = {
      annotations.contains(implicitly[ClassTag[A]].runtimeClass)
    }

    override def following[A : ClassTag](region: Span): Iterator[(Span, A)] = {
      val annotations = selectAnnotations[A]
      var pos = BinarySearch.interpolationSearch(annotations, (_:(Span, Any))._1.begin, region.end)
      if(pos < 0) pos = ~pos
      annotations.view(pos, annotations.length).iterator
    }

    override def preceding[A : ClassTag](region: Span): Iterator[(Span, A)] = {
      val annotations = selectReverse[A]
      var pos = BinarySearch.interpolationSearch(annotations, (_:(Span, Any))._1.end, region.begin + 1)
      if(pos < 0) pos = ~pos
      annotations.view(0, pos).reverseIterator
    }

    override def covered[A  : ClassTag](region: Span): IndexedSeq[(Span, A)] = {
      val annotations = selectAnnotations[A]
      var begin = BinarySearch.interpolationSearch(annotations, (_:(Span, Any))._1.begin, region.begin)
      if(begin < 0) begin = ~begin
      var end = annotations.indexWhere(_._1.end > region.end, begin)
      if(end < 0) end = annotations.length
      annotations.slice(begin, end)
    }

    override def iterator[A : ClassTag]: Iterator[(Span, A)] = {
      selectAnnotations[A].iterator
    }

    override def indexedSeq[A : ClassTag]: IndexedSeq[(Span, A)] = {
      selectAnnotations[A]
    }

    private def selectAnnotations[A : ClassTag]: IndexedSeq[(Span, A)] = {
      annotations.getOrElse(implicitly[ClassTag[A]].runtimeClass, IndexedSeq.empty).asInstanceOf[IndexedSeq[(Span, A)]]
    }

    private def selectReverse[A : ClassTag]:  IndexedSeq[(Span, A)] = {
      reverseAnnotations.getOrElse(implicitly[ClassTag[A]].runtimeClass, IndexedSeq.empty).asInstanceOf[IndexedSeq[(Span, A)]]
    }

    /*
    override def stringRep[A >: AnnotationType: ClassTag] = {
      iterator[A].map { case (Span(begin, end), x) => s"Span($begin, $end) $x"}.mkString("\n")
    }
    */

  }

}