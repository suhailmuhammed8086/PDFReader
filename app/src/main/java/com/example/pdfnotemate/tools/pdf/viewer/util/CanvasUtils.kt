package com.example.pdfnotemate.tools.pdf.viewer.util

import android.graphics.PointF
import android.graphics.RectF
import androidx.core.graphics.contains
import kotlin.math.sqrt

object CanvasUtils {
    fun isCircleCollided(cx1: Float, cy1: Float, r1: Float, cx2: Float, cy2: Float, r2: Float): Boolean {
        val dx = cx1 - cx2
        val dy = cy1 - cy2
        val distance = sqrt(dx * dx + dy * dy)
        return distance <= r1 + r2
    }

    fun isCircleCollided(point1: PointF, r1: Float, point2: PointF, r2: Float): Boolean {
        val dx = point1.x - point2.x
        val dy = point1.y - point2.y
        val distance = sqrt(dx * dx + dy * dy)
        println("$dx dx")
        println("$dy dy")
        println("$distance distance")
        return distance <= r1 + r2
    }
}

fun RectF.zoom(value: Float): RectF {
    return RectF(left * value, top * value, right * value, bottom * value)
}

fun PointF.getDistance(point: PointF): Float {
    val deltaX = point.x - x
    val deltaY = point.y - y
    return sqrt(deltaX * deltaX + deltaY * deltaY)
}
