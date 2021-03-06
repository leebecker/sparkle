/*
 Copyright 2009 David Hall, Daniel Ramage

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/
package org.sparkle.preprocess

import org.sparkle.slate._
import org.sparkle.typesystem.basic.{Sentence,Token}


/**
 * Abstract trait for tokenizers, which annotate sentence-segmented text with tokens. Tokenizers work
 * with both raw strings and [[org.sparkle.slate.StringSlate]]s.
 *
 */
@SerialVersionUID(1)
trait SparkleTokenizer extends StringAnalysisFunction with Serializable with (String=>IndexedSeq[String]) {
  override def toString() = getClass.getName +"()"

  def apply(a: String):IndexedSeq[String] = {
    val slate = apply(Slate(a).append(Span(0, a.length), Sentence()))
    slate.iterator[Token].map(_._2.token).toIndexedSeq
  }

}


