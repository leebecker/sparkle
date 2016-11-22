package org.sparkle.typesystem.ops

import org.sparkle.slate.{Span, StringSlate}

import scala.reflect.ClassTag

/**
  * A window operation is used to get Windows (i.e. different annotation spans) from a slate object
  * Instantiations of WindowOps are intended to allow finer control in StringAnalysis functions.
  *
  * Potential uses include:
  * <UL>
  * <LI> Running only on spans marked as paragraphs.
  * <LI> Running only on spans marked as English.
  * </UL>
  *
  */
trait WindowOps[WINDOW] {
  def selectWindows[In <: WINDOW](slate: StringSlate): TraversableOnce[(Span, WINDOW)]
}

/**
  * Extracts all of the given window type from the slate
  * @tparam WINDOW
  */
class SimpleWindowOps[WINDOW: ClassTag] extends WindowOps[WINDOW] {

  override def selectWindows[In <: WINDOW](slate: StringSlate): TraversableOnce[(Span, WINDOW)] = {
    slate.iterator[WINDOW]
  }

}

object SimpleWindowOps {
  def apply[WINDOW] = new SimpleWindowOps[WINDOW]
}

