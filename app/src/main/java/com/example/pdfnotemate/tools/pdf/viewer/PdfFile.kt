/**
 * Copyright 2017 Bartosz Schiller
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.pdfnotemate.tools.pdf.viewer

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.util.SparseBooleanArray
import com.example.pdfnotemate.tools.pdf.viewer.exception.PageRenderingException
import com.example.pdfnotemate.tools.pdf.viewer.model.PageModel
import com.example.pdfnotemate.tools.pdf.viewer.util.FitPolicy
import com.example.pdfnotemate.tools.pdf.viewer.util.PageSizeCalculator
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfDocument.Bookmark
import com.shockwave.pdfium.PdfDocument.Meta
import com.shockwave.pdfium.PdfiumCore
import com.shockwave.pdfium.util.Size
import com.shockwave.pdfium.util.SizeF
import kotlin.math.abs

class PdfFile internal constructor(
    private val pdfiumCore: PdfiumCore?,
    private var pdfDocument: PdfDocument?,
    private val pageFitPolicy: FitPolicy,
    private val viewSize: Size,
    /**
     * The pages the user want to display in order
     * (ex: 0, 2, 2, 8, 8, 1, 1, 1)
     */
    private var originalUserPages: IntArray?,
    /** True if scrolling is vertical, else it's horizontal  */
    private val isVertical: Boolean,
    /** Fixed spacing between pages in pixels  */
    private val spacingPx: Int,
    /** Calculate spacing automatically so each page fits on it's own in the center of the view  */
    private var autoSpacing: Boolean,
    /**
     * True if every page should fit separately according to the FitPolicy,
     * else the largest page fits and other pages scale relatively
     */
    private var fitEachPage: Boolean,
    val pageDetails: ArrayList<PageModel>,
    var paginationStartPageIndex: Int = 0,
) {
    var pagesCount = 0
        private set

    var paginationEndPageIndex: Int = 0
        get() = paginationStartPageIndex + pagesCount - 1

    /** Original page sizes  */
    var originalPageSizes: MutableList<Size> = ArrayList()

    /** Scaled page sizes  */
    var pageSizes: MutableList<SizeF> = ArrayList()

    /** Opened pages with indicator whether opening was successful  */
    var openedPages = SparseBooleanArray()

    /** Page with maximum width  */
    private var originalMaxWidthPageSize = Size(0, 0)

    /** Page with maximum height  */
    private var originalMaxHeightPageSize = Size(0, 0)

    /** Scaled page with maximum height  */
    private var maxHeightPageSize = SizeF(0f, 0f)

    /** Scaled page with maximum width  */
    private var maxWidthPageSize = SizeF(0f, 0f)

    /** Calculated offsets for pages  */
    private var pageOffsets: MutableList<Float> = ArrayList()

    /** Calculated auto spacing for pages  */
    private var pageSpacing: MutableList<Float> = ArrayList()

    /** Calculated document length (width or height, depending on swipe mode)  */
    private var documentLength = 0f

    var documents = arrayListOf<PdfDocumentDetails>()

    private var mergePdfCallback: MergePdfListener? = null

    private fun getDocument(pageIndex: Int): PdfDocument {
        return (documents.find { pageIndex >= it.startIndex && pageIndex <= it.endIndex } ?: documents.first()).document
    }
    private fun getDocumentDetails(pageIndex: Int): PdfDocumentDetails? {
        return documents.find { pageIndex >= it.startIndex && pageIndex <= it.endIndex }
    }

    private fun getDocumentPageIndex(pageIndex: Int): Int {
        val docDetails = getDocumentDetails(pageIndex)
        return pageIndex - (docDetails?.startIndex ?: 0)
    }

    private fun reset() {
        pagesCount = 0
        originalPageSizes.clear()
        pageSizes.clear()
        openedPages.clear()
        originalMaxWidthPageSize = Size(0, 0)
        originalMaxHeightPageSize = Size(0, 0)
        maxHeightPageSize = SizeF(0f, 0f)
        maxWidthPageSize = SizeF(0f, 0f)
        pageOffsets.clear()
        pageSpacing.clear()
        documentLength = 0f
    }

    init {
        documents.add(PdfDocumentDetails(0, pdfiumCore!!.getPageCount(pdfDocument) - 1, pdfDocument!!, paginationStartPageIndex))
        fitEachPage = true
        setup(viewSize)
        documents.firstOrNull()?.docLength = documentLength
    }

    private fun setup(viewSize: Size) {
        pagesCount = if (originalUserPages != null) {
            originalUserPages!!.size
        } else {
            var pageCount = 0
            documents.forEach { pageCount += pdfiumCore!!.getPageCount(it.document) }
            pageCount
        }
        for (i in 0 until pagesCount) {
            val pageSize = pdfiumCore!!.getPageSize(getDocument(i), documentPage(i))

            if (pageSize.width > originalMaxWidthPageSize.width) {
                originalMaxWidthPageSize = pageSize
            }
            if (pageSize.height > originalMaxHeightPageSize.height) {
                originalMaxHeightPageSize = pageSize
            }
            originalPageSizes.add(pageSize)
        }
        recalculatePageSizes(viewSize)
    }

    /**
     * Call after view size change to recalculate page sizes, offsets and document length
     *
     * @param viewSize new size of changed view
     */
    fun recalculatePageSizes(viewSize: Size) {
        pageSizes.clear()
        val calculator = PageSizeCalculator(
            pageFitPolicy,
            originalMaxWidthPageSize,
            originalMaxHeightPageSize,
            viewSize,
            fitEachPage,
        )
        maxWidthPageSize = calculator.optimalMaxWidthPageSize
        maxHeightPageSize = calculator.optimalMaxHeightPageSize
        for (size in originalPageSizes) {
            pageSizes.add(calculator.calculate(size))
        }
        if (autoSpacing) {
            prepareAutoSpacing(viewSize)
        }

        prepareDocLen()
        preparePagesOffset()
    }

    fun getPageSize(pageIndex: Int): SizeF {
//        val docPage = documentPage(pageIndex)
//        return if (docPage < 0) {
//            SizeF(0f, 0f)
//        } else pageSizes[pageIndex]

        return pageSizes[pageIndex]
    }

    fun getScaledPageSize(pageIndex: Int, zoom: Float): SizeF {
        val size = getPageSize(pageIndex)
        return SizeF(size.width * zoom, size.height * zoom)
    }

    /**
     * get page size with biggest dimension (width in vertical mode and height in horizontal mode)
     *
     * @return size of page
     */
    private val maxPageSize: SizeF
        get() = if (isVertical) maxWidthPageSize else maxHeightPageSize
    val maxPageWidth: Float
        get() = maxPageSize.width
    val maxPageHeight: Float
        get() = maxPageSize.height

    private fun prepareAutoSpacing(viewSize: Size) {
        pageSpacing.clear()
        for (i in 0 until pagesCount) {
            val pageSize = pageSizes[i]
            var spacing = Math.max(
                0f,
                if (isVertical) viewSize.height - pageSize.height else viewSize.width - pageSize.width,
            )
            if (i < pagesCount - 1) {
                spacing += spacingPx.toFloat()
            }
            pageSpacing.add(spacing)
        }
    }

    private fun prepareDocLen() {
        var length = 0f
        for (i in 0 until pagesCount) {
            val pageSize = pageSizes[i]
            length += if (isVertical) pageSize.height else pageSize.width
            if (autoSpacing) {
                length += pageSpacing[i]
            } else if (i < pagesCount - 1) {
                length += spacingPx.toFloat()
            }
        }
        documentLength = length
    }

    private fun preparePagesOffset() {
        pageOffsets.clear()
        var offset = 0f
        for (i in 0 until pagesCount) {
            val pageSize = pageSizes[i]
            val size = if (isVertical) pageSize.height else pageSize.width
            if (autoSpacing) {
                offset += pageSpacing[i] / 2f
                if (i == 0) {
                    offset -= spacingPx / 2f
                } else if (i == pagesCount - 1) {
                    offset += spacingPx / 2f
                }
                pageOffsets.add(offset)
                offset += size + pageSpacing[i] / 2f
            } else {
                pageOffsets.add(offset)
                offset += size + spacingPx
            }
        }
    }

    fun getDocLen(zoom: Float): Float {
        return documentLength * zoom
    }

    /**
     * Get the page's height if swiping vertical, or width if swiping horizontal.
     */
    fun getPageLength(pageIndex: Int, zoom: Float): Float {
        val size = getPageSize(pageIndex)
        return (if (isVertical) size.height else size.width) * zoom
    }

    private fun getPageSpacing(pageIndex: Int, zoom: Float): Float {
        val spacing = if (autoSpacing) pageSpacing[pageIndex] else spacingPx.toFloat()
        return spacing * zoom
    }

    /** Get primary page offset, that is Y for vertical scroll and X for horizontal scroll  */
    fun getPageOffsetWithZoom(pageIndex: Int, zoom: Float): Float {
        return pageOffsets[pageIndex] * zoom
    }

    /** Get primary page offset, that is Y for vertical scroll and X for horizontal scroll  */
    fun getPageOffset(pageIndex: Int): Float {
        return pageOffsets.getOrNull(pageIndex) ?: 0f
    }

    fun getPageCurrentOffset(pageIndex: Int, zoom: Float, yOffset: Float): Float {
        val offset = pageOffsets[pageIndex] * zoom
        return (offset + yOffset)
    }

    /** Get secondary page offset, that is X for vertical scroll and Y for horizontal scroll  */
    fun getSecondaryPageOffset(pageIndex: Int, zoom: Float): Float {
        val pageSize = getPageSize(pageIndex)
        return if (isVertical) {
            val maxWidth = maxPageWidth
            zoom * (maxWidth - pageSize.width) / 2 // x
        } else {
            val maxHeight = maxPageHeight
            zoom * (maxHeight - pageSize.height) / 2 // y
        }
    }

    fun getPageAtOffset(offset: Float, zoom: Float): Int {
        var currentPage = 0
        for (i in 0 until pagesCount) {
            val off = pageOffsets[i] * zoom - getPageSpacing(i, zoom) / 2f
            if (off >= offset) {
                break
            }
            currentPage++
        }
        return if (--currentPage >= 0) currentPage else 0
    }

    @Throws(PageRenderingException::class)
    fun openPage(pageIndex: Int): Boolean {
        val docPage = documentPage(pageIndex)
        if (docPage < 0) {
            return false
        }
        val paginationIndex = getPaginationIndexFromPageIndex(pageIndex)
        synchronized(lock) {
            return if ((openedPages.indexOfKey(paginationIndex) < 0)) {
                try {
                    pdfiumCore!!.openPage(getDocument(pageIndex), docPage)
                    openedPages.put(paginationIndex, true)
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    openedPages.put(paginationIndex, false)
                    throw PageRenderingException(pageIndex, e)
                }
            } else {
                false
            }
        }
    }

    fun pageHasError(pageIndex: Int): Boolean {
        val page = getPaginationIndexFromPageIndex(pageIndex)
        return !openedPages[page, false]
    }

    fun renderPageBitmap(
        bitmap: Bitmap?,
        pageIndex: Int,
        bounds: Rect,
        annotationRendering: Boolean,
    ) {
        val docPage = documentPage(pageIndex)
        val document = getDocument(pageIndex)
        pdfiumCore!!.renderPageBitmap(
            document,
            bitmap,
            docPage,
            bounds.left,
            bounds.top,
            bounds.width(),
            bounds.height(),
            annotationRendering,
        )
    }

    val metaData: Meta?
        get() = if (pdfDocument == null) {
            null
        } else {
            pdfiumCore!!.getDocumentMeta(pdfDocument)
        }
    val bookmarks: List<Bookmark>
        get() = if (pdfDocument == null) {
            ArrayList()
        } else {
            pdfiumCore!!.getTableOfContents(pdfDocument)
        }

    fun getPageLinks(pageIndex: Int): List<PdfDocument.Link> {
        val docPage = documentPage(pageIndex)
        return pdfiumCore!!.getPageLinks(getDocument(pageIndex), docPage)
    }

    fun mapRectToDevice(
        pageIndex: Int,
        startX: Int,
        startY: Int,
        sizeX: Int,
        sizeY: Int,
        rect: RectF?,
    ): RectF {
        val docPage = documentPage(pageIndex)
        return pdfiumCore!!.mapRectToDevice(
            getDocument(pageIndex),
            docPage,
            startX,
            startY,
            sizeX,
            sizeY,
            0,
            rect,
        )
    }

    fun dispose() {
        documents.forEach { pdfiumCore?.closeDocument(it.document) }
        pdfDocument = null
        openedPages.clear()
        originalUserPages = null
    }

    fun prepareCharsRelativeSize() {
        for (i in 0 until pagesCount) {
            val size = pageSizes[i]
            val pdfWidth = pageDetails[i].width
            val pdfHeight = pageDetails[i].height
            val xRatio = getXRatio(pdfWidth, size.width)
            var xOffset = 0f
            val widthDiff = maxPageWidth - size.width
            if (widthDiff > 0) { xOffset = widthDiff / 2 }
            var yOffset = 0f
            val heightDiff = maxPageHeight - size.height
            if (heightDiff > 0) { yOffset = heightDiff / 2 }
            val yRatio = getYRatio(pdfHeight, size.height)
            pageDetails[i].coordinates.forEach { line ->
                val rx = line.position.x * xRatio + xOffset
                val ry = line.position.y * yRatio + yOffset
                val rWidth = line.size.width * xRatio
                val rHeight = line.size.height * yRatio
                line.relatedPosition.set(rx, ry)
                line.relatedSize.set(rWidth, rHeight)
                line.rect.set(rx, ry, rx + rWidth, ry + rHeight)
                line.words.forEach { word ->
                    val rx2 = word.position.x * xRatio + xOffset
                    val ry2 = word.position.y * yRatio + yOffset
                    val rWidth2 = word.size.width * xRatio
                    val rHeight2 = word.size.height * yRatio
                    word.relatedPosition.set(rx2, ry2)
                    word.relatedSize.set(rWidth2, rHeight2)
                    word.rect.set(rx2, ry2, rx2 + rWidth2, ry2 + rHeight2)
                    word.characters.forEach { char ->
                        val rx3 = char.topPosition.x * xRatio + xOffset
                        val ry3 = char.topPosition.y * yRatio + yOffset
                        val rWidth3 = char.size.width * xRatio
                        val rHeight3 = char.size.height * yRatio
                        char.relatedPosition.set(rx3, ry3)
                        char.relatedSize.set(rWidth3, rHeight3)
                        char.rect.set(rx3, ry3, rx3 + rWidth3, ry3 + rHeight3)
                    }
                }
            }
            pageDetails[i].relativeSizeCalculated = true
        }
    }

    private fun getXRatio(pdfWidth: Float, viewWidth: Float): Float {
        return (1 / pdfWidth) * viewWidth
    }

    private fun getYRatio(pdfHeight: Float, viewHeight: Float): Float {
        return (1 / pdfHeight) * viewHeight
    }

    /**
     * Given the UserPage number, this method restrict it
     * to be sure it's an existing page. It takes care of
     * using the user defined pages if any.
     *
     * @param userPage A page number.
     * @return A restricted valid page number (example : -2 => 0)
     */
    fun determineValidPageNumberFrom(userPage: Int): Int {
        if (userPage <= 0) { return 0 }
        if (originalUserPages != null) {
            if (userPage >= originalUserPages!!.size) { return originalUserPages!!.size - 1 }
        } else {
            if (userPage >= pagesCount) { return pagesCount - 1 }
        }
        return userPage
    }

    private fun documentPage(userPage: Int): Int {
        var documentPage = userPage
        if (originalUserPages != null) {
            documentPage = if (userPage < 0 || userPage >= originalUserPages!!.size) {
                return -1
            } else {
                originalUserPages!![userPage]
            }
        }
        return if (documentPage < 0 || userPage >= pagesCount) {
            -1
        } else {
            getDocumentPageIndex(documentPage)
        }
    }

    fun getPageDetails(): List<PageModel> {
        return pageDetails
    }
    fun getPageDetail(pageIndex: Int): PageModel {
        return pageDetails[pageIndex]
    }
    fun setMergeListener(listener: MergePdfListener) {
        this.mergePdfCallback = listener
    }

    /**Get the page index related to pagination, not in the showing order*/
    fun getPaginationIndexFromPageIndex(pageIndex: Int): Int {
        return paginationStartPageIndex + pageIndex
    }

    /**Get the page index in drawn order,*/
    fun getPageIndexFromPaginationIndex(paginationPage: Int): Int {
        return paginationPage - paginationStartPageIndex
    }

    fun mergeDataAtTop(
        mergeId: Int,
        newPdf: PdfDocument,
        startPage: Int,
        pageDetails: ArrayList<PageModel>,
        pageToLoad: Int,
    ) {
        val pdfFile = PdfFile(pdfiumCore, newPdf, pageFitPolicy, viewSize, originalUserPages, isVertical, spacingPx, autoSpacing, fitEachPage, pageDetails)
        val documentsDetails = arrayListOf<PdfDocumentDetails>()
        documentsDetails.addAll(documents.toList())
//        val removedPdfDocLength = adjustDocumentList(documentsDetails, MergeType.TOP)
        val newPdfPageCount = pdfiumCore!!.getPageCount(newPdf)
        documentsDetails.forEach {
            it.startIndex += (newPdfPageCount)
            it.endIndex += (newPdfPageCount)
        }
        documentsDetails.add(0, PdfDocumentDetails(0, newPdfPageCount - 1, newPdf, startPage))
        pdfFile.setUpMergeData(mergeId, this, documentsDetails, startPage, MergeType.TOP, pageToLoad, 0f)
    }

    fun mergeDataAtBottom(
        mergeId: Int,
        newPdf: PdfDocument,
        startPage: Int,
        pageDetails: ArrayList<PageModel>,
        pageToLoad: Int,
    ) {
        val pdfFile = PdfFile(pdfiumCore, newPdf, pageFitPolicy, viewSize, originalUserPages, isVertical, spacingPx, autoSpacing, fitEachPage, pageDetails)
        val documentsDetails = arrayListOf<PdfDocumentDetails>()
        documentsDetails.addAll(documents.toList())
        val lastDocEndIndex = documentsDetails.lastOrNull()?.endIndex ?: -1
        val newPdfPageCount = pdfiumCore!!.getPageCount(newPdf)
        val startIndex = lastDocEndIndex + 1
        val endIndex = lastDocEndIndex + newPdfPageCount
        documentsDetails.add(PdfDocumentDetails(startIndex, endIndex, newPdf, startPage))
        pdfFile.setUpMergeData(
            mergeId,
            this,
            documentsDetails,
            startPage,
            MergeType.BOTTOM,
            pageToLoad,
        )
    }

    private fun setUpMergeData(
        mergeId: Int,
        parentPdfFile: PdfFile,
        documentsDetails: ArrayList<PdfDocumentDetails>,
        startPage: Int,
        mergeType: MergeType,
        pageToLoad: Int,
        removedChunkLength: Float = 0f,
    ) {
        val initialLength = parentPdfFile.documentLength
        documents = documentsDetails
        reset()
        setup(viewSize)
        val mergedFileLength = abs(documentLength - initialLength - removedChunkLength)
        when (mergeType) {
            MergeType.TOP -> {
                documentsDetails.firstOrNull()?.docLength = mergedFileLength
            }
            MergeType.BOTTOM -> {
                documentsDetails.lastOrNull()?.docLength = mergedFileLength
            }
        }
        parentPdfFile.attachMergeData(
            mergeId,
            this,
            mergeType,
            startPage,
            mergedFileLength,
            pageToLoad,
        )
    }

    private fun attachMergeData(
        mergeId: Int,
        mergedPdfFile: PdfFile,
        mergeType: MergeType,
        mergePdfStartPage: Int,
        mergedFileDocSize: Float,
        pageToLoad: Int,
    ) {
        pagesCount = mergedPdfFile.pagesCount
        originalPageSizes = mergedPdfFile.originalPageSizes
        pageSizes = mergedPdfFile.pageSizes
        if (mergeType == MergeType.TOP) {
            openedPages.clear()
            paginationStartPageIndex = mergePdfStartPage
            pageDetails.addAll(0, mergedPdfFile.pageDetails)
        } else {
            pageDetails.addAll(mergedPdfFile.pageDetails)
        }
        originalMaxWidthPageSize = mergedPdfFile.originalMaxWidthPageSize
        originalMaxHeightPageSize = mergedPdfFile.originalMaxHeightPageSize
        maxHeightPageSize = mergedPdfFile.maxHeightPageSize
        maxWidthPageSize = mergedPdfFile.maxWidthPageSize
        pageOffsets = mergedPdfFile.pageOffsets
        pageSpacing = mergedPdfFile.pageSpacing
        documentLength = mergedPdfFile.documentLength
        documents = mergedPdfFile.documents
        prepareCharsRelativeSize()
        mergePdfCallback?.mergeEnd(mergeId, mergeType, mergedFileDocSize, pageToLoad)
    }

    data class PdfDocumentDetails(
        var startIndex: Int,
        var endIndex: Int,
        val document: PdfDocument,
        val paginationStartPage: Int,
    ) {
        var docLength = 0f
    }

    interface MergePdfListener {
        fun mergeStart(mergeType: MergeType)
        fun mergeEnd(
            mergeId: Int,
            mergeType: MergeType,
            mergedFileDocLength: Float,
            pageToLoad: Int,
        )
    }

    enum class MergeType {
        TOP, BOTTOM
    }

    companion object {
        private val lock = Any()
    }
}
