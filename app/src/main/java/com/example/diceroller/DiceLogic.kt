package com.example.diceroller

object DiceLogic {
    fun calculateHoldDelay(holdLevel: Int): Long {
        return (500 / holdLevel + 15).toLong().coerceAtLeast(16L)
    }

    fun calculateSegmentIndex(holdLevel: Int, maxHoldLevel: Int, animSeqSize: Int): Int {
        return (((holdLevel - 1).toFloat() / (maxHoldLevel - 1)) * animSeqSize)
            .toInt()
            .coerceIn(0, animSeqSize - 1)
    }

    fun calculateSeed(t: Int, holdLevel: Int): Int {
        return (t + holdLevel).coerceAtLeast(1)
    }

    fun calculateNextI(currentI: Int, seed: Int): Int {
        return ((currentI.toLong() * seed) % 20 + 1).toInt()
    }

    fun calculateFaceDelay(holdLevel: Int, face: Int, maxFaces: Int): Long {
        val denominator = (maxFaces + 1) - face
        return if (denominator > 0) {
            ((2 + 1500 / holdLevel) / denominator).toLong().coerceAtLeast(16L)
        } else {
            16L
        }
    }
}