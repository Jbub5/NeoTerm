package com.neoterm.utils

/**
 * @author kiva
 */
object StringDistance {
  fun distance(source: String, target: String): Int {
    val sources = source.toCharArray()
    val targets = target.toCharArray()
    val sourceLen = sources.size
    val targetLen = targets.size

    val d = Array(sourceLen + 1) { IntArray(targetLen + 1) }
    for (i in 0..sourceLen) {
      d[i][0] = i
    }
    for (i in 0..targetLen) {
      d[0][i] = i
    }

    for (i in 1..sourceLen) {
      for (j in 1..targetLen) {
        d[i][j] = if (sources[i - 1] == targets[j - 1]) {
          d[i - 1][j - 1]
        } else {
          val insert = d[i][j - 1] + 1
          val delete = d[i - 1][j] + 1
          val replace = d[i - 1][j - 1] + 1
          minOf(insert, delete, replace)
        }
      }
    }
    return d[sourceLen][targetLen]
  }
}
