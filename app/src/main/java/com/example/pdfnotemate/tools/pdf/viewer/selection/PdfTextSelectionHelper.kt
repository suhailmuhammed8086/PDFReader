package com.example.pdfnotemate.tools.pdf.viewer.selection

import android.graphics.*
import com.example.pdfnotemate.tools.pdf.viewer.model.TextSelectionData

class PdfTextSelectionHelper {
    private val selectedTextPaint = Paint().apply {
        this.color = Color.parseColor("#FA5D3B")
        this.alpha = 50
        this.style = Paint.Style.FILL
        this.isAntiAlias = true
    }
    private val selectionHandleColor = Paint().apply {
        this.color = Color.parseColor("#FA5D3B")
        this.style = Paint.Style.FILL_AND_STROKE
        this.isAntiAlias = true
    }
    val strokeColorRed = Paint().apply {
        this.color = Color.parseColor("#ff4185")
        this.style = Paint.Style.STROKE
        this.strokeWidth = 1f
        this.isAntiAlias = true
        this.textSize = 14f
    }
    val strokeColorGreen = Paint().apply {
        this.color = Color.GREEN
        this.style = Paint.Style.STROKE
        this.strokeWidth = 1f
        this.isAntiAlias = true
    }

    // Handle
    val handleRoundRadius = 20f

    var startHandlePosition = PointF()
    var endHandlePosition = PointF()
    var startCharHeight = 0f

    var startSelectionPosition = PointF()
    var endSelectionPosition = PointF()
    var endCharHeight = 0f

    var touchState: TouchState = TouchState.IDLE
    enum class TouchState { StartHandlePressed, EndHandlePressed, IDLE }

    private fun drawStartHandle(canvas: Canvas, x: Float, y: Float, zoom: Float) {
        val mR = handleRoundRadius * zoom
        val mX = x * zoom
        val mY = y * zoom
        canvas.drawCircle(mX - mR, mY + mR, mR, selectionHandleColor)
        val path = Path()
        path.moveTo(mX, mY)
        path.lineTo(mX - mR, mY)
        path.lineTo(mX, mY + mR)
        path.close()
        canvas.drawPath(path, selectionHandleColor)
    }
    private fun drawEndHandle(canvas: Canvas, x: Float, y: Float, zoom: Float) {
        val mR = handleRoundRadius * zoom
        val mX = x * zoom
        val mY = y * zoom
        canvas.drawCircle(mX + mR, mY + mR, mR, selectionHandleColor)
        val path = Path()
        path.moveTo(mX, mY)
        path.lineTo(mX + mR, mY)
        path.lineTo(mX, mY + mR)
        path.close()
        canvas.drawPath(path, selectionHandleColor)
    }

    fun drawSelection(startY: Float, canvas: Canvas, details: TextSelectionData, zoom: Float) {
        val selectionDetail = details.getSelections()
        canvas.translate(0f, startY * zoom)
        selectionDetail.forEachIndexed { index, data ->
            if (index == 0) {
                data.startChar.relatedPosition.let {
                    drawStartHandle(canvas, it.x, data.rect.bottom, zoom)
                    startHandlePosition.set(it.x - handleRoundRadius / 2, it.y + data.startChar.relatedSize.height + handleRoundRadius / 2)
                    startSelectionPosition.set(it.x, it.y + data.startChar.relatedSize.height / 2)
                    startCharHeight = data.startChar.relatedSize.height
                }
            }
            if (index == selectionDetail.lastIndex) {
                data.endChar.let {
                    drawEndHandle(canvas, it.relatedPosition.x + it.relatedSize.width, data.rect.bottom, zoom)
                    endHandlePosition.set(it.relatedPosition.x + handleRoundRadius / 2, it.relatedPosition.y + it.relatedSize.height + handleRoundRadius / 2)
                    endSelectionPosition.set(it.relatedPosition.x, it.relatedPosition.y + it.relatedSize.height / 2)
                    startCharHeight = it.relatedSize.height
                }
            }
            canvas.drawRect(data.rect.zoom(zoom), selectedTextPaint)
        }
        canvas.translate(0f, -startY * zoom)
    }

    private fun RectF.zoom(value: Float): RectF {
        return RectF(left * value, top * value, right * value, bottom * value)
    }

    // testPoints
//    var userPoint = PointF()
//    var xAxisPaint = Paint().apply {
//        color = Color.RED
//        strokeWidth = 2f
//        style = Paint.Style.STROKE
//    }
//    var yAxisPaint = Paint().apply {
//        color = Color.GREEN
//        strokeWidth = 2f
//        style = Paint.Style.STROKE
//    }
}
