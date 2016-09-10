package org.sparkle.util

object BinarySearch {

  /**
    * Linear search backwards from starting index
    */
  private def findFirst[T](objs: IndexedSeq[T], proj: T=>Int, toFind: Int, startingIndex: Int): Int = {
    var i = startingIndex
    var lastSeenIdx = i

    while (proj(objs(i)) == toFind && i > 0) {
      lastSeenIdx = i
      i -= 1
    }
    lastSeenIdx
  }

  /**
    * Interpolation search is a search algorithm with Average Complexity log(log(n)).  For indexes of spans it is more efficient than
    * a regular binary search.
    *
    * This implentation varies from the standard algorithm in the following ways:
    * 1) Returns a negative values whose compliment is the index of the first value greater than the specified number
    * 2) Supports indexed sequences with duplicate values.  It accomplishes this by performing a linear search after finding
    * the specified value to find the first occurrence of said value.
    *
    * For more details see [[https://en.wikipedia.org/wiki/Interpolation_search the Wikipedia article on Interpolation Search]].
    *
    * @param objs - collection of indexed objects
    * @param proj - function mapping object to an Int key
    * @param toFind - Int key to find
    * @tparam T
    * @return - Returns index of specified key.  If not found, returns a negative number whose compliment is the first value greater
    *         than the specified key.
    */
  def interpolationSearch[T](objs: IndexedSeq[T], proj: T=>Int, toFind: Int):Int = {
    val getVal = (i: Int) => proj(objs(i))
    if (objs.isEmpty) return ~0

    // Returns index of toFind in sortedArray, or -1 if not found
    var low = 0
    var lowV = getVal(low)
    var high = objs.length - 1
    var highV = getVal(high)

    while (lowV <= toFind && highV >= toFind) {
      val mid = (if(highV == lowV) low else low + ((toFind - lowV.toLong) * (high - low)) / (highV.toLong - lowV.toLong)).toInt

      val midV = getVal(mid)
      if (midV < toFind) {
        low = mid + 1
        lowV = getVal(low)
      } else if (midV > toFind) {
        high = mid - 1
        highV = getVal(high)
      } else {
        return findFirst(objs, proj, toFind, mid)
      }
    }

    if (lowV == toFind) {
      findFirst(objs, proj, toFind, low)
    } else if(lowV > toFind) {
      ~low
    } else {
      ~(high + 1)
    }
  }



}

