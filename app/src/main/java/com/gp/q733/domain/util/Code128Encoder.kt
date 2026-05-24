package com.gp.q733.domain.util

/**
 * CODE_128 barcode encoder
 * Encodes a string into a list of bar widths (true=black, false=white)
 * Each element represents one module width
 */
object Code128Encoder {

    // CODE_128B patterns: each value is a list of 6 widths (bar, space, bar, space, bar, space)
    // Values are 1-4 modules wide
    private val PATTERNS = arrayOf(
        intArrayOf(2,1,2,2,2,2), // 0: space
        intArrayOf(2,2,2,1,2,2), // 1: !
        intArrayOf(2,2,2,2,2,1), // 2: "
        intArrayOf(1,2,1,2,2,3), // 3: #
        intArrayOf(1,2,1,3,2,2), // 4: $
        intArrayOf(1,3,1,2,2,2), // 5: %
        intArrayOf(1,2,2,2,1,3), // 6: &
        intArrayOf(1,2,2,3,1,2), // 7: '
        intArrayOf(1,3,2,2,1,2), // 8: (
        intArrayOf(2,2,1,2,1,3), // 9: )
        intArrayOf(2,2,1,3,1,2), // 10: *
        intArrayOf(2,3,1,2,1,2), // 11: +
        intArrayOf(1,1,2,2,3,2), // 12: ,
        intArrayOf(1,2,2,1,3,2), // 13: -
        intArrayOf(1,2,2,2,3,1), // 14: .
        intArrayOf(1,1,3,2,2,2), // 15: /
        intArrayOf(1,2,3,1,2,2), // 16: 0
        intArrayOf(1,2,3,2,2,1), // 17: 1
        intArrayOf(2,2,3,2,1,1), // 18: 2
        intArrayOf(2,2,1,1,3,2), // 19: 3
        intArrayOf(2,2,1,2,3,1), // 20: 4
        intArrayOf(2,1,3,2,1,2), // 21: 5
        intArrayOf(2,2,3,1,1,2), // 22: 6
        intArrayOf(3,1,2,1,3,1), // 23: 7
        intArrayOf(3,1,1,2,2,2), // 24: 8
        intArrayOf(3,2,1,1,2,2), // 25: 9
        intArrayOf(3,2,1,2,2,1), // 26: :
        intArrayOf(3,1,2,2,1,2), // 27: ;
        intArrayOf(3,2,2,1,1,2), // 28: <
        intArrayOf(3,2,2,2,1,1), // 29: =
        intArrayOf(2,1,2,1,2,3), // 30: >
        intArrayOf(2,1,2,3,2,1), // 31: ?
        intArrayOf(2,3,2,1,2,1), // 32: @
        intArrayOf(1,1,1,3,2,3), // 33: A
        intArrayOf(1,3,1,1,2,3), // 34: B
        intArrayOf(1,3,1,3,2,1), // 35: C
        intArrayOf(1,1,2,3,1,3), // 36: D
        intArrayOf(1,3,2,1,1,3), // 37: E
        intArrayOf(1,3,2,3,1,1), // 38: F
        intArrayOf(2,1,1,3,1,3), // 39: G
        intArrayOf(2,3,1,1,1,3), // 40: H
        intArrayOf(2,3,1,3,1,1), // 41: I
        intArrayOf(1,1,2,1,3,3), // 42: J
        intArrayOf(1,1,2,3,3,1), // 43: K
        intArrayOf(1,3,2,1,3,1), // 44: L
        intArrayOf(1,1,3,1,2,3), // 45: M
        intArrayOf(1,1,3,3,2,1), // 46: N
        intArrayOf(1,3,3,1,2,1), // 47: O
        intArrayOf(3,1,3,1,2,1), // 48: P
        intArrayOf(2,1,1,3,3,1), // 49: Q
        intArrayOf(2,3,1,1,3,1), // 50: R
        intArrayOf(2,1,3,1,1,3), // 51: S
        intArrayOf(2,1,3,3,1,1), // 52: T
        intArrayOf(2,1,3,1,3,1), // 53: U
        intArrayOf(3,1,1,1,2,3), // 54: V
        intArrayOf(3,1,1,3,2,1), // 55: W
        intArrayOf(3,3,1,1,2,1), // 56: X
        intArrayOf(3,1,2,1,1,3), // 57: Y
        intArrayOf(3,1,2,3,1,1), // 58: Z
        intArrayOf(3,3,2,1,1,1), // 59: [
        intArrayOf(3,1,4,1,1,1), // 60: \
        intArrayOf(2,2,1,4,1,1), // 61: ]
        intArrayOf(4,3,1,1,1,1), // 62: ^
        intArrayOf(1,1,1,2,2,4), // 63: _
        intArrayOf(1,1,1,4,2,2), // 64: `
        intArrayOf(1,2,1,1,2,4), // 65: a
        intArrayOf(1,2,1,4,2,1), // 66: b
        intArrayOf(1,4,1,1,2,2), // 67: c
        intArrayOf(1,4,1,2,2,1), // 68: d
        intArrayOf(1,1,2,2,1,4), // 69: e
        intArrayOf(1,1,2,4,1,2), // 70: f
        intArrayOf(1,2,2,1,1,4), // 71: g
        intArrayOf(1,2,2,4,1,1), // 72: h
        intArrayOf(1,4,2,1,1,2), // 73: i
        intArrayOf(1,4,2,2,1,1), // 74: j
        intArrayOf(2,4,1,2,1,1), // 75: k
        intArrayOf(2,2,1,1,1,4), // 76: l
        intArrayOf(4,1,3,1,1,1), // 77: m
        intArrayOf(2,4,1,1,1,2), // 78: n
        intArrayOf(1,3,4,1,1,1), // 79: o
        intArrayOf(1,1,1,2,4,2), // 80: p
        intArrayOf(1,2,1,1,4,2), // 81: q
        intArrayOf(1,2,1,2,4,1), // 82: r
        intArrayOf(1,1,4,2,1,2), // 83: s
        intArrayOf(1,2,4,1,1,2), // 84: t
        intArrayOf(1,2,4,2,1,1), // 85: u
        intArrayOf(4,1,1,2,1,2), // 86: v
        intArrayOf(4,2,1,1,1,2), // 87: w
        intArrayOf(4,2,1,2,1,1), // 88: x
        intArrayOf(2,1,2,1,4,1), // 89: y
        intArrayOf(2,1,4,1,2,1), // 90: z
        intArrayOf(4,1,2,1,2,1), // 91: {
        intArrayOf(1,1,1,1,4,3), // 92: |
        intArrayOf(1,1,1,3,4,1), // 93: }
        intArrayOf(1,3,1,1,4,1), // 94: ~
        intArrayOf(1,1,4,1,1,3), // 95: DEL
        intArrayOf(1,1,4,3,1,1), // 96: FNC3
        intArrayOf(4,1,1,1,1,3), // 97: FNC2
        intArrayOf(4,1,1,3,1,1), // 98: SHIFT
        intArrayOf(1,1,3,1,4,1), // 99: CODE_C
        intArrayOf(1,1,4,1,3,1), // 100: CODE_B (FNC4)
        intArrayOf(4,1,1,1,3,1), // 101: CODE_A (FNC4)
        intArrayOf(3,1,1,1,4,1), // 102: FNC1
        intArrayOf(4,1,1,1,3,1), // 103: Start A
        intArrayOf(2,1,1,4,1,2), // 104: Start B (most common)
        intArrayOf(2,1,1,2,1,4), // 105: Start C
        intArrayOf(2,3,3,1,1,1), // 106: Stop
    )

    // CODE_128B: character to value mapping (ASCII 32-127 → value 0-95)
    private fun charToValue(c: Char): Int {
        val code = c.code
        return when {
            code in 32..127 -> code - 32
            else -> 0 // fallback to space
        }
    }

    /**
     * Encode a string into a list of bar segments
     * @return list of pairs (widthInModules, isBlack)
     */
    fun encode(text: String): List<Pair<Int, Boolean>> {
        if (text.isBlank()) return emptyList()

        val segments = mutableListOf<Pair<Int, Boolean>>()
        val values = mutableListOf<Int>()

        // Start B
        values.add(104)
        segments.addAll(patternToSegments(PATTERNS[104]))

        // Data characters (CODE_128B)
        for (c in text) {
            val v = charToValue(c)
            values.add(v)
            segments.addAll(patternToSegments(PATTERNS[v]))
        }

        // Checksum
        var checksum = values[0]
        for (i in 1 until values.size) {
            checksum += values[i] * i
        }
        checksum %= 103
        segments.addAll(patternToSegments(PATTERNS[checksum]))

        // Stop
        segments.addAll(patternToSegments(PATTERNS[106]))

        return segments
    }

    /**
     * Encode and return as a flat list of boolean values (true=black, false=white)
     * Each element = one module width
     */
    fun encodeToModules(text: String): List<Boolean> {
        val segments = encode(text)
        val modules = mutableListOf<Boolean>()
        for ((width, isBlack) in segments) {
            repeat(width) { modules.add(isBlack) }
        }
        return modules
    }

    private fun patternToSegments(pattern: IntArray): List<Pair<Int, Boolean>> {
        val segments = mutableListOf<Pair<Int, Boolean>>()
        for (i in pattern.indices) {
            segments.add(Pair(pattern[i], i % 2 == 0)) // even=bar(Black), odd=space(White)
        }
        return segments
    }
}
