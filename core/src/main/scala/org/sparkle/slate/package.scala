package org.sparkle

/**
  * Created by leebecker on 4/14/16.
  */
package object slate {
  /**
    *  Function which accepts Slates with content type [[String]], and region type [[org.sparkle.slate.Span Span]]
    */
  type StringAnalysisFunction = AnalysisFunction[String, Span]
  type StringSlate = Slate[String, Span]

  /**
    * Useful trait for writing functions which accept Slate objects and extracts
    * analysis of form SCHEMA
    *
    * @tparam SCHEMA Type of analysis extracted
    */
  trait StringSlateExtractor[SCHEMA] extends Serializable {
    def apply(slate: StringSlate): SCHEMA
  }
}