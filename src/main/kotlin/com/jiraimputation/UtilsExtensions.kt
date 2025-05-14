package com.jiraimputation

/**
 * Splits the iterable into contiguous chunks based on a predicate between consecutive elements.
 *
 * Each chunk will contain adjacent elements that satisfy the given condition.
 * A new chunk is started every time the predicate returns false between the last element
 * of the current chunk and the next item.
 *
 * This function uses a single iterator instance to avoid restarting iteration,
 * which would happen if you called `this.iterator()` multiple times.
 *
 * @param predicate A function that takes two elements (previous, next) and returns true
 *                  if they should be in the same chunk, false otherwise.
 * @return A list of chunks (lists of elements) based on the predicate condition.
 *
 * @sample
 * ```kotlin
 * val input = listOf(1, 2, 3, 10, 11, 20)
 * val result = input.chunkWhile { a, b -> b - a <= 1 }
 * // result = [[1, 2, 3], [10, 11], [20]]
 * ```
 *
 * @sample
 * ```kotlin
 * val logs = listOf(
 *     BranchLog("2025-05-14T07:00:00Z", "JIR-1"),
 *     BranchLog("2025-05-14T07:05:00Z", "JIR-1"),
 *     BranchLog("2025-05-14T07:20:00Z", "JIR-1") // gap too big
 * )
 * val grouped = logs.chunkWhile { a, b ->
 *     Instant.parse(a.timestamp).until(Instant.parse(b.timestamp), DateTimeUnit.MINUTE) <= 5
 * }
 * // grouped.size == 2
 * ```
 */
fun <T> Iterable<T>.chunkWhile(predicate: (T, T) -> Boolean): List<List<T>> {
    val iterator = this.iterator()
    if (!iterator.hasNext()) return emptyList()

    val result = mutableListOf<MutableList<T>>()
    var currentChunk = mutableListOf<T>().apply { add(iterator.next()) }

    while (iterator.hasNext()) {
        val item = iterator.next()
        if (predicate(currentChunk.last(), item)) {
            currentChunk.add(item)
        } else {
            result.add(currentChunk)
            currentChunk = mutableListOf(item)
        }
    }

    result.add(currentChunk)
    return result
}

