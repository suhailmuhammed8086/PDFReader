package com.example.pdfnotemate.tools.pdf.viewer.model

import android.graphics.PointF
import android.graphics.RectF

class PdfLine(
    val id: Int,
    val text: String,
    val position: PointF,
    val size: Size,
) {
    var relatedPosition = PointF()
    var relatedSize = Size(0f, 0f)

    val words: ArrayList<PdfWord> = arrayListOf()
    val rect: RectF = RectF()
}
class PdfWord(
    val id: Int,
    val lineId: Int,
    val text: String,
    val position: PointF,
    val size: Size,
) {
    var relatedPosition = PointF()
    var relatedSize = Size(0f, 0f)

    val characters: ArrayList<PdfChar> = arrayListOf()
    val rect: RectF = RectF()
}

data class PdfChar(
    val id: Int,
    val lineId: Int,
    val wordId: Int,
    val text: String,
    /**Y calculated from top position , (0,0) from top-left position*/
    val topPosition: PointF,
    /**Y calculated from bottom position , (0,0) from bottom-left position*/
    val bottomPosition: PointF,
    val size: Size,
    val pageNumber: Int,
) {
    var relatedPosition = PointF()
    var relatedSize = Size(0f, 0f)

    val rect: RectF = RectF()

//    var bottomY = 0f
}

class Size(var width: Float = 0f, var height: Float = 0f) {
    fun set(width: Float, height: Float) {
        this.width = width
        this.height = height
    }
}
