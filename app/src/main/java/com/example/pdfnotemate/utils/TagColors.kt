package com.example.pdfnotemate.utils

import android.graphics.Color

class TagColors {
    private val topicColors = listOf(
        "#389271",
        "#FD7C36",
        "#FF6C6C",
        "#9278DF",
        "#2487F7",
        "#6E398A",
        "#454F99",
        "#2C72AF",
        "#0896BA",
        "#008D5B",
        "#8BBB3F",
        "#FCC612",
    )

    fun getColor(id: Long): Int {
        val index = (id % topicColors.size).toInt()
        return Color.parseColor(topicColors[index])
    }
}