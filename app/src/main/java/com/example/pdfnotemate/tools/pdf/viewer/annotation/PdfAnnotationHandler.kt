package com.example.pdfnotemate.tools.pdf.viewer.annotation

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.TypedValue
import androidx.core.content.res.ResourcesCompat
import com.example.pdfnotemate.R
import com.example.pdfnotemate.tools.pdf.viewer.model.HighlightModel
import com.example.pdfnotemate.tools.pdf.viewer.model.CommentModel
import com.example.pdfnotemate.tools.pdf.viewer.model.PdfAnnotationModel
import com.example.pdfnotemate.tools.pdf.viewer.util.CanvasUtils
import com.example.pdfnotemate.tools.pdf.viewer.util.zoom

class PdfAnnotationHandler(
    context: Context?,
    resource: Resources,
) {
    var annotations = arrayListOf<PdfAnnotationModel>()
    private var noteColor = Color.parseColor("#FA5D3B") // note underline color

    private var noteStampBitmap: Bitmap = BitmapFactory.decodeResource(resource, R.drawable.ic_note_stamp)
    private var stampWidth = 35f
    private var stampHeight = 35f
    private var stampX = 0f
    private var addedNoteStampDetails = HashMap<Int, List<AddedStampDetails>>()

    init {
        stampWidth = getDpValue(resource, 20f).toFloat()
        stampHeight = stampWidth
    }

    private val outerCirclePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL_AND_STROKE
    }
    private val innerCirclePaint = Paint().apply {
        color = Color.parseColor("#057FE0")
        style = Paint.Style.FILL_AND_STROKE
    }

    val textSize = getDpValue(resource, 8f)
    val textPaint = Paint().apply {
        color = Color.WHITE
        typeface = context?.let { ResourcesCompat.getFont(it, R.font.jakarta_sans_regular_400) }
    }

    fun drawAnnotations(paginationPageIndexes: List<Int>, pageOffsets: List<Float>, canvas: Canvas, zoom: Float) {
        addedNoteStampDetails.clear()
        stampX = canvas.width - (stampWidth * 1.5).toFloat()
        for (annotation in annotations) {
            val index = paginationPageIndexes.indexOfFirst { it == annotation.paginationPageIndex }
            if (index == -1) continue
            val pageOffset = pageOffsets[index]
            canvas.translate(0f, pageOffset * zoom)
            try {
                when (annotation.type) {
                    PdfAnnotationModel.Type.Note -> drawNoteAnnotations(canvas, zoom, annotation.asNote()!!, index)
                    PdfAnnotationModel.Type.Highlight -> drawHighlights(canvas, zoom, annotation.asHighlight()!!)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            canvas.translate(0f, -pageOffset * zoom)
        }
        // Drawing note badge and number
        for (i in paginationPageIndexes.indices) {
            val pageOffset = pageOffsets[i]
            canvas.translate(0f, pageOffset * zoom)
            val noteStamps = addedNoteStampDetails[i] ?: emptyList()
            noteStamps.forEach {
                val count = it.noteIds.size
                drawNoteStamp(canvas, it.y, zoom, count)
            }
            canvas.translate(0f, -pageOffset * zoom)
        }
    }

    private fun drawNoteAnnotations(canvas: Canvas, zoom: Float, note: CommentModel, mainPageIndex: Int) {
        val paint = Paint().apply {
            color = noteColor
            style = Paint.Style.STROKE
            strokeWidth = 2f * zoom
        }
        val selectionDetail = note.charDrawSegments
        selectionDetail.forEachIndexed { index, data ->
            val rect = data.rect.zoom(zoom)
            if (index == 0) {
                // storing note start position , we will later use this value to draw badge and number
                val middleY = (data.rect.top + data.rect.bottom) / 2
                addNoteStampDetails(middleY, mainPageIndex, note.id.toInt())
            }
            canvas.drawLine(
                rect.left,
                rect.bottom,
                rect.right,
                rect.bottom,
                paint,
            )
        }
    }
    private fun addNoteStampDetails(y: Float, page: Int, noteId: Int) {
        val alreadyAddedYs = ArrayList(addedNoteStampDetails[page] ?: emptyList())
        val addedStamp = alreadyAddedYs.filter {
            it.y == y
        }
        if (addedStamp.isEmpty()) {
            // there no stamp exist already , so  we add new one
            val stampDetails = AddedStampDetails(y).also { it.noteIds.add(noteId) }
            alreadyAddedYs.add(stampDetails)
            addedNoteStampDetails[page] = alreadyAddedYs
        } else {
            // already badge in that position, so we will add the note id
            addedStamp.firstOrNull()?.noteIds?.add(noteId)
        }
    }
    private fun drawNoteStamp(canvas: Canvas, y: Float, zoom: Float, count: Int) {
        val my = y - stampHeight / 2 // to make the stamp vertically center to the line
        val destRect = RectF(stampX, my, stampX + stampWidth, my + stampHeight).zoom(zoom)
        canvas.drawBitmap(noteStampBitmap, null, destRect, Paint())

        if (count > 1) {
            // draw count on the stamp
            val circleRadius = stampWidth * 0.30f * zoom
            val cX = destRect.right - circleRadius / 2
            val cY = destRect.top + circleRadius / 2
            canvas.drawCircle(cX, cY, circleRadius, outerCirclePaint)
            canvas.drawCircle(cX, cY, circleRadius * 0.80f, innerCirclePaint)

            textPaint.textSize = textSize * zoom
            val textWidth = textPaint.measureText(count.toString())
            val fontMetrics = textPaint.fontMetrics
            val textHeight = fontMetrics.descent - fontMetrics.ascent
            canvas.drawText(count.toString(), cX - (textWidth / 2), cY + (textHeight * 0.25f), textPaint)
        }
    }

    private fun drawHighlights(canvas: Canvas, zoom: Float, highlight: HighlightModel) {
        val paint = Paint().apply {
            color = Color.parseColor(highlight.color)
            style = Paint.Style.FILL
            alpha = 50
        }
        val selectionDetail = highlight.charDrawSegments
        selectionDetail.forEach { data ->
//            canvas.drawRect(data.rect.zoom(zoom), paint)
            canvas.drawRoundRect(data.rect.zoom(zoom), 2f, 2f, paint)
        }
    }

    fun findAnnotationOnPoint(paginationPageIndex: Int, point: PointF): PdfAnnotationModel? {
        for (annotation in annotations) {
            if (annotation.paginationPageIndex != paginationPageIndex) continue
            annotation.charDrawSegments.forEach {
                if (it.rect.contains(point.x, point.y)) {
                    return annotation
                }
            }
        }
        return null
    }
    fun findHighlightOnPoint(point: PointF): HighlightModel? {
        for (annotation in annotations) {
            if (annotation.type != PdfAnnotationModel.Type.Highlight) { continue }
            annotation.charDrawSegments.forEach {
                if (it.rect.contains(point.x, point.y)) {
                    return annotation.asHighlight()
                }
            }
        }
        return null
    }
    fun findNoteOnPoint(point: PointF): CommentModel? {
        for (annotation in annotations) {
            if (annotation.type != PdfAnnotationModel.Type.Note) { continue }
            annotation.charDrawSegments.forEach {
                if (it.rect.contains(point.x, point.y)) {
                    return annotation.asNote()
                }
            }
        }
        return null
    }

    fun findNoteStampOnPoint(point: PointF, paginationPageIndex: Int): List<CommentModel> {
        val stampPoint = PointF(stampX + (stampWidth / 2), 0f)
        val resultNotes = ArrayList<CommentModel>()
        for (annotation in annotations) {
            if (annotation.type != PdfAnnotationModel.Type.Note || annotation.paginationPageIndex != paginationPageIndex) {
                continue
            }
            val firstLine = annotation.charDrawSegments.firstOrNull()
            if (firstLine != null) {
                stampPoint.y = (firstLine.rect.top + firstLine.rect.bottom) / 2
                if (CanvasUtils.isCircleCollided(stampPoint, 1f, point, stampWidth / 2f)) {
                    resultNotes.add(annotation.asNote()!!)
                }
            }
        }
        return resultNotes
    }

    fun removeCommentAnnotation(noteIds: List<Long>) {
        annotations.removeAll {
            it.type == PdfAnnotationModel.Type.Note && noteIds.contains(it.asNote()!!.id)
        }
    }
    fun removeHighlightAnnotation(highlightIds: List<Long>) {
        annotations.removeAll {
            it.type == PdfAnnotationModel.Type.Highlight && highlightIds.contains(it.asHighlight()!!.id)
        }
    }

    fun getNoteAnnotation(noteId: Int): CommentModel? {
        return annotations.find { it.type == PdfAnnotationModel.Type.Note && it.asNote() != null && it.asNote()!!.id.toInt() == noteId }?.asNote()
    }

    fun getDpValue(resource: Resources, dpValue: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dpValue,
            resource.displayMetrics,
        ).toInt()
    }

    /**This function itself will not clear the annotations that are already drawn,
     * you will need to redraw to apply this changes
     * */
    fun clearAllAnnotations() {
        annotations.clear()
    }

    data class AddedStampDetails(
        val y: Float,
        val noteIds: ArrayList<Int> = arrayListOf(),
    )

    companion object {
        private const val TAG = "AnnotationHandler"
    }
}
