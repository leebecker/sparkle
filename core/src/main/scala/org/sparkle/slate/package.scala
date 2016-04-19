package org.sparkle

/**
  * Created by leebecker on 4/14/16.
  */
package object slate {
  type StringAnalysisFunction = AnalysisFunction[String, Span]
  type StringSlate = Slate[String, Span]
}