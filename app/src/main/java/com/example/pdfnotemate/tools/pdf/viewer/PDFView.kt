/**
 * Copyright 2016 Bartosz Schiller
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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Paint.Style
import android.graphics.PaintFlagsDrawFilter
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.HandlerThread
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.RelativeLayout
import com.example.pdfnotemate.tools.pdf.viewer.annotation.PdfAnnotationHandler
import com.example.pdfnotemate.tools.pdf.viewer.exception.PageRenderingException
import com.example.pdfnotemate.tools.pdf.viewer.link.DefaultLinkHandler
import com.example.pdfnotemate.tools.pdf.viewer.link.LinkHandler
import com.example.pdfnotemate.tools.pdf.viewer.listener.Callbacks
import com.example.pdfnotemate.tools.pdf.viewer.listener.OnDrawListener
import com.example.pdfnotemate.tools.pdf.viewer.listener.OnErrorListener
import com.example.pdfnotemate.tools.pdf.viewer.listener.OnLoadCompleteListener
import com.example.pdfnotemate.tools.pdf.viewer.listener.OnLongPressListener
import com.example.pdfnotemate.tools.pdf.viewer.listener.OnPageChangeListener
import com.example.pdfnotemate.tools.pdf.viewer.listener.OnPageErrorListener
import com.example.pdfnotemate.tools.pdf.viewer.listener.OnPageScrollListener
import com.example.pdfnotemate.tools.pdf.viewer.listener.OnRenderListener
import com.example.pdfnotemate.tools.pdf.viewer.listener.OnTapListener
import com.example.pdfnotemate.tools.pdf.viewer.model.CharDrawSegments
import com.example.pdfnotemate.tools.pdf.viewer.model.HighlightModel
import com.example.pdfnotemate.tools.pdf.viewer.model.CommentModel
import com.example.pdfnotemate.tools.pdf.viewer.model.PageModel
import com.example.pdfnotemate.tools.pdf.viewer.model.PagePart
import com.example.pdfnotemate.tools.pdf.viewer.model.PdfAnnotationModel
import com.example.pdfnotemate.tools.pdf.viewer.model.PdfChar
import com.example.pdfnotemate.tools.pdf.viewer.model.PdfLine
import com.example.pdfnotemate.tools.pdf.viewer.model.PdfWord
import com.example.pdfnotemate.tools.pdf.viewer.model.TextSelectionData
import com.example.pdfnotemate.tools.pdf.viewer.scroll.ScrollHandle
import com.example.pdfnotemate.tools.pdf.viewer.selection.PdfTextSelectionHelper
import com.example.pdfnotemate.tools.pdf.viewer.source.ByteArraySource
import com.example.pdfnotemate.tools.pdf.viewer.source.DocumentSource
import com.example.pdfnotemate.tools.pdf.viewer.source.FileSource
import com.example.pdfnotemate.tools.pdf.viewer.util.CanvasUtils
import com.example.pdfnotemate.tools.pdf.viewer.util.Constants
import com.example.pdfnotemate.tools.pdf.viewer.util.FitPolicy
import com.example.pdfnotemate.tools.pdf.viewer.util.MathUtils
import com.example.pdfnotemate.tools.pdf.viewer.util.SnapEdge
import com.example.pdfnotemate.tools.pdf.viewer.util.Util
import com.example.pdfnotemate.tools.pdf.viewer.util.zoom
import com.example.pdfnotemate.utils.log
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfDocument.Bookmark
import com.shockwave.pdfium.PdfDocument.Meta
import com.shockwave.pdfium.PdfiumCore
import com.shockwave.pdfium.util.Size
import com.shockwave.pdfium.util.SizeF
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * It supports animations, zoom, cache, and swipe.
 *
 *
 * To fully understand this class you must know its principles :
 * - The PDF document is seen as if we always want to draw all the pages.
 * - The thing is that we only draw the visible parts.
 * - All parts are the same size, this is because we can't interrupt a native page rendering,
 * so we need these renderings to be as fast as possible, and be able to interrupt them
 * as soon as we can.
 * - The parts are loaded when the current offset or the current zoom level changes
 *
 *
 * Important :
 * - DocumentPage = A page of the PDF document.
 * - UserPage = A page as defined by the user.
 * By default, they're the same. But the user can change the pages order
 * using [.load]. In this
 * particular case, a userPage of 5 can refer to a documentPage of 17.
 */
class PDFView(context: Context?, set: AttributeSet?) :
    RelativeLayout(context, set),
    DragPinchManager.Listener,
    PdfFile.MergePdfListener {
    var minZoom = DEFAULT_MIN_SCALE
    var midZoom = DEFAULT_MID_SCALE
    var maxZoom = DEFAULT_MAX_SCALE

    /**
     * START - scrolling in first page direction
     * END - scrolling in last page direction
     * NONE - not scrolling
     */
    internal enum class ScrollDir {
        NONE, START, END
    }

    private var scrollDir = ScrollDir.NONE

    /** Rendered parts go to the cache manager  */
    var cacheManager: CacheManager? = null

    /** Animation manager manage all offset and zoom animation  */
    private var animationManager: AnimationManager? = null

    /** Drag manager manage all touch events  */
    private var dragPinchManager: DragPinchManager? = null

    var pdfFile: PdfFile? = null
    var file: File? = null

    /** The index of the current sequence  */
    var currentPage = 0
        private set

    /**
     * If you picture all the pages side by side in their optimal width,
     * and taking into account the zoom level, the current offset is the
     * position of the left border of the screen in this big picture
     */
    var currentXOffset = 0f
        private set

    /**
     * If you picture all the pages side by side in their optimal width,
     * and taking into account the zoom level, the current offset is the
     * position of the left border of the screen in this big picture
     */
    var currentYOffset = 0f
        private set

    /** The zoom level, always >= 1  */
    var zoom = 1f
        private set

    /** True if the PDFView has been recycled  */
    private var isRecycled = true

    /** Current state of the view  */
    private var state = State.DEFAULT

    /**Which side is merging
     *  TOP_MERGING_LOADING - Merging at top position,
     *  BOTTOM_MERGE_LOADING - Merging at bottom position,
     *  IDLE - No Merging
     * */
    enum class MergeState { TOP_MERGING_LOADING, BOTTOM_MERGE_LOADING, IDLE }
    private var mergeState: MergeState = MergeState.IDLE

    // this variable is created to create a unique mergeId , every time we add 1 to this value to create new MergeId
    private var currentMergeId: Int = 1

    private var totalPdfPage = -1

    /** decode pdf file  */
    private var pdfDecoder: PdfDecoder? = null

    /** The thread [.renderingHandler] will run on  */
    private var renderingHandlerThread: HandlerThread?

    /** Handler always waiting in the background and rendering tasks  */
    var renderingHandler: RenderingHandler? = null
    private var pagesLoader: PagesLoader? = null

    @JvmField
    var callbacks = Callbacks()

    /** Paint object for drawing  */
    private var paint: Paint = Paint()

    /** Paint object for drawing debug stuff  */
    private var debugPaint: Paint = Paint()

    /** Policy for fitting pages to screen  */
    var pageFitPolicy = FitPolicy.BOTH
        private set
    var isFitEachPage = false
        private set
    private var defaultPage = 0

    /** True if should scroll through pages vertically instead of horizontally  */
    var isSwipeVertical = true
        private set
    var isSwipeEnabled = true
    var isDoubleTapEnabled = true
        private set
    private var nightMode = false
    var isPageSnap = true

    /** Pdfium core for loading and rendering PDFs  */
    private var pdfiumCore: PdfiumCore? = null
    var scrollHandle: ScrollHandle? = null
        private set
    private var isScrollHandleInit = false

    /**
     * True if bitmap should use ARGB_8888 format and take more memory
     * False if bitmap should be compressed by using RGB_565 format and take less memory
     */
    var isBestQuality = false
        private set

    /**
     * True if annotations should be rendered
     * False otherwise
     */
    var isAnnotationRendering = false
        private set

    /**
     * True if the view should render during scaling<br></br>
     * Can not be forced on older API versions (< Build.VERSION_CODES.KITKAT) as the GestureDetector does
     * not detect scrolling while scaling.<br></br>
     * False otherwise
     */
    private var renderDuringScale = false

    /** Antialiasing and bitmap filtering  */
    var isAntialiasing = true
        private set
    private val antialiasFilter =
        PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    /** Spacing between pages, in px  */
    var spacingPx = 0
        private set

    /** Add dynamic spacing to fit each page separately on the screen.  */
    var isAutoSpacingEnabled = false
        private set

    /** Fling a single page at a time  */
    var isPageFlingEnabled = true
        private set

    /** Pages numbers used when calling onDrawAllListener  */
    private val onDrawPagesNumbs: MutableList<Int> = ArrayList(10)

    /** Holds info whether view has been added to layout and has width and height  */
    private var hasSize = false

    /** Holds last used Configurator that should be loaded when view has size  */
    private var waitingDocumentConfigurator: Configurator? = null

    /** To vibrate on word selection*/
    private var vibratorManager: VibratorManager? = null
    private var vibrator: Vibrator? = null

    private var textSelectionHelper = PdfTextSelectionHelper()
    private val textSelection = TextSelectionData()
    private var listener: Listener? = null
    private var scope: CoroutineScope? = null
    private val annotationHandler = PdfAnnotationHandler(context, resources)

    /** To draw lines between pages*/
    private val pageDivisionPaint = Paint().apply {
        this.color = Color.GRAY
        this.strokeWidth = 2f
        this.style = Style.STROKE
    }

    /** Construct the initial view  */
    init {
        renderingHandlerThread = HandlerThread("PDF renderer")
        if (!isInEditMode) {
            cacheManager = CacheManager()
            animationManager = AnimationManager(this)
            dragPinchManager = DragPinchManager(this, animationManager!!, this)
            pagesLoader = PagesLoader(this)
            debugPaint.style = Style.STROKE
            pdfiumCore = PdfiumCore(context)
            setWillNotDraw(false)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibratorManager = context?.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        } else {
            vibrator = context?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        }
    }

    /**Get unique id for this pdfView instance*/
    private fun getMergeId(): Int {
        currentMergeId += 1
        return currentMergeId
    }

    @SuppressLint("NewApi")
    private fun vibrate(seconds: Long = 50L) {
        if (vibratorManager != null) {
            val vibe = VibrationEffect.createOneShot(seconds, VibrationEffect.DEFAULT_AMPLITUDE)
            vibratorManager?.defaultVibrator?.vibrate(vibe)
        } else if (vibrator != null && vibrator?.hasVibrator() == true) {
            val vibe = VibrationEffect.createOneShot(seconds, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator!!.vibrate(vibe)
        }
    }

    fun setListener(listener: Listener): PDFView {
        this.listener = listener
        return this
    }

    /** Set the total number of the pages in the pdf. it is the number of pages of pdfChunks*/
    fun setTotalPdfPageCount(pageCount: Int): PDFView {
        this.totalPdfPage = pageCount
        return this
    }

    /** You must call this function before loading pdf*/
    fun attachCoroutineScope(scope: CoroutineScope): PDFView {
        this.scope = scope
        return this
    }

    private fun load(docSource: DocumentSource, password: String?) {
        check(isRecycled) { "Don't call load on a PDF View without recycling it first." }
        listener?.onPreparationStarted()
        isRecycled = false
        pdfDecoder = PdfDecoder(this, pdfiumCore)
        scope?.launch(Dispatchers.IO) {
            pdfDecoder?.decode(docSource, password)
        }
    }

    /**
     * Go to the given page.
     *
     * @param page Page index.
     */
    @JvmOverloads
    fun jumpTo(page: Int, withAnimation: Boolean = false, resetZoom: Boolean = false, resetHorizontalScroll: Boolean = false) {
        var mPage = page
        if (pdfFile == null) {
            return
        }
        if (resetZoom) {
            zoom = 1f
        }
        if (resetHorizontalScroll) {
            currentXOffset = 0f
        }
        mPage = pdfFile!!.determineValidPageNumberFrom(mPage)
        val offset: Float = if (mPage == 0) 0f else -pdfFile!!.getPageOffsetWithZoom(mPage, zoom)
        if (isSwipeVertical) {
            if (withAnimation) {
                animationManager?.startYAnimation(currentYOffset, offset)
            } else {
                moveTo(currentXOffset, offset)
            }
        } else {
            if (withAnimation) {
                animationManager?.startXAnimation(currentXOffset, offset)
            } else {
                moveTo(offset, currentYOffset)
            }
        }
        showPage(mPage)
    }

    /** Redirecting to the given pageIndex , this index is not pdfChunk index*/
    fun jumpToWithPaginationPageIndex(paginationPageIndex: Int, withAnimation: Boolean = false, resetZoom: Boolean = false, resetHorizontalScroll: Boolean = false) {
        if (pdfFile == null) {
            return
        }
        val page = pdfFile!!.getPageIndexFromPaginationIndex(paginationPageIndex)
        if (page >= 0) {
            jumpTo(page, withAnimation, resetZoom, resetHorizontalScroll)
        }
    }
    fun showPage(page: Int) {
        var pageNb = page
        if (isRecycled) {
            return
        }

        // Check the page number and makes the
        // difference between UserPages and DocumentPages
        pageNb = pdfFile!!.determineValidPageNumberFrom(pageNb)
        currentPage = pageNb
        loadPages()
        if (scrollHandle != null && !documentFitsView()) {
            scrollHandle!!.setPageNum(currentPage + 1)
        }
        callbacks.callOnPageChange(currentPage, pdfFile!!.pagesCount)
        scope?.launch(Dispatchers.Main) { listener?.onPageChanged(currentPage, pdfFile!!.getPaginationIndexFromPageIndex(currentPage)) }
    }

    /**
     * Get current position as ratio of document length to visible area.
     * 0 means that document start is visible, 1 that document end is visible
     *
     * @return offset between 0 and 1
     */
    var positionOffset: Float
        get() {
            val offset: Float = if (isSwipeVertical) {
                -currentYOffset / (pdfFile!!.getDocLen(zoom) - height)
            } else {
                -currentXOffset / (pdfFile!!.getDocLen(zoom) - width)
            }
            return MathUtils.limit(offset, 0f, 1f)
        }
        set(progress) {
            setPositionOffset(progress, true)
        }

    /**
     * @param progress   must be between 0 and 1
     * @param moveHandle whether to move scroll handle
     */
    fun setPositionOffset(progress: Float, moveHandle: Boolean) {
        if (isSwipeVertical) {
            moveTo(currentXOffset, (-pdfFile!!.getDocLen(zoom) + height) * progress, moveHandle)
        } else {
            moveTo((-pdfFile!!.getDocLen(zoom) + width) * progress, currentYOffset, moveHandle)
        }
        loadPageByOffset()
    }

    fun stopFling() {
        animationManager?.stopFling()
    }

    val pageCount: Int
        get() = if (pdfFile == null) {
            0
        } else {
            pdfFile!!.pagesCount
        }

    fun setNightMode(nightMode: Boolean) {
        this.nightMode = nightMode
        if (nightMode) {
            val colorMatrixInverted = ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
            val filter = ColorMatrixColorFilter(colorMatrixInverted)
            paint.colorFilter = filter
        } else {
            paint.colorFilter = null
        }
    }

    fun enableDoubleTap(enableDoubleTap: Boolean) {
        isDoubleTapEnabled = enableDoubleTap
    }

    fun onPageError(ex: PageRenderingException) {
        if (!callbacks.callOnPageError(ex.page, ex.cause)) {
            Log.e(TAG, "Cannot open page " + ex.page, ex.cause)
        }
    }

    fun recycle() {
        waitingDocumentConfigurator = null
        animationManager?.stopAll()
        dragPinchManager?.disable()

        // Stop tasks
        if (renderingHandler != null) {
            renderingHandler!!.stop()
            renderingHandler!!.removeMessages(RenderingHandler.MSG_RENDER_TASK)
        }

        pdfDecoder?.cancel()

        // Clear caches
        cacheManager?.recycle()
        if (scrollHandle != null && isScrollHandleInit) {
            scrollHandle!!.destroyLayout()
        }
        if (pdfFile != null) {
            pdfFile!!.dispose()
            pdfFile = null
        }
        renderingHandler = null
        scrollHandle = null
        isScrollHandleInit = false
        currentYOffset = 0f
        currentXOffset = currentYOffset
        zoom = 1f
        isRecycled = true
        callbacks = Callbacks()
        state = State.DEFAULT
        mergeState = MergeState.IDLE
    }

    /** Handle fling animation  */
    override fun computeScroll() {
        super.computeScroll()
        if (isInEditMode) {
            return
        }
        animationManager?.computeFling()
    }

    override fun onDetachedFromWindow() {
        recycle()
        if (renderingHandlerThread != null) {
            renderingHandlerThread!!.quitSafely()
            renderingHandlerThread = null
        }
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        hasSize = true
        if (waitingDocumentConfigurator != null) {
            waitingDocumentConfigurator!!.load()
        }
        if (isInEditMode || state != State.SHOWN) {
            return
        }

        // calculates the position of the point which in the center of view relative to big strip
        val centerPointInStripXOffset = -currentXOffset + oldw * 0.5f
        val centerPointInStripYOffset = -currentYOffset + oldh * 0.5f
        val relativeCenterPointInStripXOffset: Float
        val relativeCenterPointInStripYOffset: Float
        if (isSwipeVertical) {
            relativeCenterPointInStripXOffset = centerPointInStripXOffset / pdfFile!!.maxPageWidth
            relativeCenterPointInStripYOffset =
                centerPointInStripYOffset / pdfFile!!.getDocLen(zoom)
        } else {
            relativeCenterPointInStripXOffset =
                centerPointInStripXOffset / pdfFile!!.getDocLen(zoom)
            relativeCenterPointInStripYOffset = centerPointInStripYOffset / pdfFile!!.maxPageHeight
        }
        animationManager?.stopAll()
        pdfFile!!.recalculatePageSizes(Size(w, h))
        if (isSwipeVertical) {
            currentXOffset = -relativeCenterPointInStripXOffset * pdfFile!!.maxPageWidth + w * 0.5f
            currentYOffset =
                -relativeCenterPointInStripYOffset * pdfFile!!.getDocLen(zoom) + h * 0.5f
        } else {
            currentXOffset =
                -relativeCenterPointInStripXOffset * pdfFile!!.getDocLen(zoom) + w * 0.5f
            currentYOffset = -relativeCenterPointInStripYOffset * pdfFile!!.maxPageHeight + h * 0.5f
        }
        moveTo(currentXOffset, currentYOffset)
        loadPageByOffset()
    }

    override fun canScrollHorizontally(direction: Int): Boolean {
        if (pdfFile == null) {
            return true
        }
        if (isSwipeVertical) {
            if (direction < 0 && currentXOffset < 0) {
                return true
            } else if (direction > 0 && currentXOffset + toCurrentScale(pdfFile!!.maxPageWidth) > width) {
                return true
            }
        } else {
            if (direction < 0 && currentXOffset < 0) {
                return true
            } else if (direction > 0 && currentXOffset + pdfFile!!.getDocLen(zoom) > width) {
                return true
            }
        }
        return false
    }

    override fun canScrollVertically(direction: Int): Boolean {
        if (pdfFile == null) {
            return true
        }
        if (isSwipeVertical) {
            if (direction < 0 && currentYOffset < 0) {
                return true
            } else if (direction > 0 && currentYOffset + pdfFile!!.getDocLen(zoom) > height) {
                return true
            }
        } else {
            if (direction < 0 && currentYOffset < 0) {
                return true
            } else if (direction > 0 && currentYOffset + toCurrentScale(pdfFile!!.maxPageHeight) > height) {
                return true
            }
        }
        return false
    }

    override fun onLongPressed(x: Float, y: Float) {
        findWordInSelectedPoint(x, y)
        redraw()
    }

    private var touchInPoint: PointF? = null

    override fun onTouch(e: MotionEvent): Boolean {
        val yOffset = e.y - currentYOffset
        val pageIndex = pdfFile!!.getPageAtOffset(yOffset, zoom)
        val pageOffset = pdfFile!!.getPageOffset(pageIndex)
        val mX = e.x.scaledX() / zoom
        val mY = (e.y.scaledY() / zoom) - pageOffset
        val point = PointF(mX, mY)
        val touchYPointTop = 2 // (2 / zoom).coerceAtLeast(1f)
        val selectionPoint = when (textSelectionHelper.touchState) {
            PdfTextSelectionHelper.TouchState.StartHandlePressed -> PointF(
                mX,
                mY - (textSelectionHelper.handleRoundRadius * touchYPointTop),
            )

            PdfTextSelectionHelper.TouchState.EndHandlePressed -> PointF(
                mX - textSelectionHelper.handleRoundRadius,
                mY - (textSelectionHelper.handleRoundRadius * touchYPointTop),
            )

            PdfTextSelectionHelper.TouchState.IDLE -> PointF(mX, mY - (textSelectionHelper.handleRoundRadius * 3))
        }
        var touchConsumed = false
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                touchInPoint = point
                if (CanvasUtils.isCircleCollided(
                        mX,
                        mY,
                        textSelectionHelper.handleRoundRadius,
                        textSelectionHelper.startHandlePosition.x,
                        textSelectionHelper.startHandlePosition.y,
                        textSelectionHelper.handleRoundRadius,
                    )
                ) {
                    textSelectionHelper.touchState =
                        PdfTextSelectionHelper.TouchState.StartHandlePressed
                    touchConsumed = true
                } else if (CanvasUtils.isCircleCollided(
                        mX,
                        mY,
                        textSelectionHelper.handleRoundRadius,
                        textSelectionHelper.endHandlePosition.x,
                        textSelectionHelper.endHandlePosition.y,
                        textSelectionHelper.handleRoundRadius,
                    )
                ) {
                    textSelectionHelper.touchState =
                        PdfTextSelectionHelper.TouchState.EndHandlePressed
                    touchConsumed = true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                when (textSelectionHelper.touchState) {
                    PdfTextSelectionHelper.TouchState.StartHandlePressed -> {
                        touchConsumed = true
                        findCharactersInTheSelectedPositions(
                            selectionPoint,
                            textSelectionHelper.endSelectionPosition,
                        )
                    }

                    PdfTextSelectionHelper.TouchState.EndHandlePressed -> {
                        touchConsumed = true
                        findCharactersInTheSelectedPositions(
                            textSelectionHelper.startSelectionPosition,
                            selectionPoint,
                        )
                    }

                    else -> {}
                }
            }

            MotionEvent.ACTION_UP -> {
                when (textSelectionHelper.touchState) {
                    PdfTextSelectionHelper.TouchState.StartHandlePressed -> {
                        findCharactersInTheSelectedPositions(
                            selectionPoint,
                            textSelectionHelper.endSelectionPosition,
                        )
                        textSelectionHelper.touchState = PdfTextSelectionHelper.TouchState.IDLE
                        touchConsumed = true
                    }

                    PdfTextSelectionHelper.TouchState.EndHandlePressed -> {
                        findCharactersInTheSelectedPositions(
                            textSelectionHelper.startSelectionPosition,
                            selectionPoint,
                        )
                        textSelectionHelper.touchState = PdfTextSelectionHelper.TouchState.IDLE
                        touchConsumed = true
                    }

                    else -> {
                        // checking user clicked on pdfNote stamp
                        if (touchInPoint != null) {
                            if (CanvasUtils.isCircleCollided(touchInPoint!!, 10f, point, 10f)) {
                                val notesOnPoint = annotationHandler.findNoteStampOnPoint(
                                    point,
                                    pdfFile!!.getPaginationIndexFromPageIndex(pageIndex),
                                )
                                if (notesOnPoint.isNotEmpty()) {
                                    showNoteAnnotationPopup(notesOnPoint)
                                    touchConsumed = true
                                }
                            }
                        }
                    }
                }

                // Passing selected text details through listener
                if (textSelection.hasTextSelected() && dragPinchManager?.isScrolling() != true) {
                    onTextSelected()
                } else if (!textSelection.hasTextSelected()) {
                    // if there is no text selected
                    clearAllTextSelectionAndCoordinates()
                }
            }
        }
        return touchConsumed
    }

    private fun onTextSelected() {
        val page = pdfFile!!.getPageIndexFromPaginationIndex(textSelection.currentSelectionPaginationIndex)
        val currentOffset = pdfFile!!.getPageCurrentOffset(page, zoom, currentYOffset) // getting current y offset of the page that have this notes
        val firstSelection = textSelection.getSelections().firstOrNull()
        if (firstSelection != null) {
            val x = (firstSelection.rect.left)
            val y = currentOffset + (firstSelection.rect.top - firstSelection.rect.height()) * zoom
            val point = PointF(x, y)
            listener?.onTextSelected(textSelection, point)
        }
    }

    private fun showNoteAnnotationPopup(notesOnPoint: List<CommentModel>) {
        if (zoom != 1f) {
            zoomWithAnimation(1f) {
                calculateNotePosition(notesOnPoint)
            }
        } else {
            calculateNotePosition(notesOnPoint)
        }
    }

//    fun showNoteAnnotationPopup(noteId: Int) {
//        val noteAnnotation = annotationHandler.getNoteAnnotation(noteId)
//        if (noteAnnotation != null) {
//            showNoteAnnotationPopup(listOf(noteAnnotation))
//        }
//    }

    private fun calculateNotePosition(notesOnPoint: List<CommentModel>) {
        notesOnPoint.first().paginationPageIndex.log("noteOnPoint")
        if (pdfFile == null) return
        val sortedNote = notesOnPoint.sortedByDescending { it.id } // sorting notes, so the last one will show first
        val note = sortedNote.first() // we are only show th point of the first note now
        val page = pdfFile!!.getPageIndexFromPaginationIndex(note.paginationPageIndex)
        val currentOffset = pdfFile!!.getPageCurrentOffset(page, 1f, currentYOffset) // getting current y offset of the page that have this notes
        val firstLineRect = note.charDrawSegments.firstOrNull()?.rect // getting the position of the first drawing segment
        if (firstLineRect != null) {
            val x = (firstLineRect.left + firstLineRect.right) / 2
            val y = currentOffset + firstLineRect.top
            val point = PointF(x, y)
            listener?.onNotesStampsClicked(sortedNote, point)
        }
    }

    override fun onTap(e: MotionEvent) {
        clearAllTextSelection(true)
        listener?.onTap()
    }

    override fun onScrollingBegin() {
        listener?.hideTextSelectionOptionWindow()
    }

    override fun onScrollingEnd() {
        if (textSelection.hasTextSelected()) {
            onTextSelected()
        }
    }

    private fun findWordInSelectedPoint(x: Float, y: Float) {
        val yOffset = y - currentYOffset
        val pageIndex = pdfFile!!.getPageAtOffset(yOffset, zoom)
        val pageOffset = pdfFile!!.getPageOffset(pageIndex)
        clearAllTextSelectionAndCoordinates()
        textSelection.currentSelectionPageIndex = pageIndex
        textSelection.currentSelectionPaginationIndex = pdfFile!!.getPaginationIndexFromPageIndex(pageIndex)
        val mX = x.scaledX() / zoom
        val mY = y.scaledY() / zoom
        val selectedWord = getWordInPoint(pageIndex, mX, mY - pageOffset)

        if (selectedWord != null) {
            textSelection.addSelection(selectedWord.characters)
            vibrate()
        }
    }

    private fun getWordInPoint(pageIndex: Int, x: Float, y: Float): PdfWord? {
        pdfFile?.pageDetails?.getOrNull(pageIndex)?.let { page ->
            for (line in page.coordinates) {
                for (word in line.words) {
                    if (word.rect.contains(x, y)) {
                        return word
                    }
                }
            }
        }
        return null
    }

    private fun rearrangeStartEndPoints(startPoint: PointF, endPoint: PointF): Boolean {
        when (textSelectionHelper.touchState) {
            PdfTextSelectionHelper.TouchState.StartHandlePressed -> {
                if (startPoint.y > endPoint.y + textSelectionHelper.endCharHeight / 2) {
                    if (startPoint.y <= endPoint.y + textSelectionHelper.endCharHeight / 2 && startPoint.y >= endPoint.y - textSelectionHelper.endCharHeight / 2) {
                        if (startPoint.x > endPoint.x) {
                            textSelectionHelper.touchState =
                                PdfTextSelectionHelper.TouchState.EndHandlePressed
                            return true
                        }
                    } else {
                        textSelectionHelper.touchState =
                            PdfTextSelectionHelper.TouchState.EndHandlePressed
                        return true
                    }
                }
            }

            PdfTextSelectionHelper.TouchState.EndHandlePressed -> {
                if (endPoint.y < startPoint.y - textSelectionHelper.startCharHeight / 2) {
                    if (endPoint.y <= startPoint.y + textSelectionHelper.startCharHeight / 2 && endPoint.y >= startPoint.y - textSelectionHelper.startCharHeight / 2) {
                        if (startPoint.x > endPoint.x) {
                            textSelectionHelper.touchState =
                                PdfTextSelectionHelper.TouchState.StartHandlePressed
                            return true
                        }
                    } else {
                        textSelectionHelper.touchState =
                            PdfTextSelectionHelper.TouchState.StartHandlePressed
                        return true
                    }
                }
            }

            else -> {}
        }

        return false
    }

    private fun findCharactersInTheSelectedPositions(startPoint: PointF, endPoint: PointF) {
        val page = textSelection.currentSelectionPageIndex
        clearAllTextSelection(false)
        val selectedLines = arrayListOf<CharDrawSegments>()

        var mStartPoint = startPoint
        var mEndPoint = endPoint

        val swapPoints = rearrangeStartEndPoints(startPoint, endPoint)
        if (swapPoints) {
            val tempStart = mStartPoint
            mStartPoint = mEndPoint
            mEndPoint = tempStart
        }

        pdfFile?.pageDetails?.getOrNull(page)?.let {
            it.coordinates.forEach { line ->
                // extracting characters inside the selection points
                // Checking all the lines that between start and end point
                if ((line.relatedPosition.y + line.relatedSize.height) >= mStartPoint.y && line.relatedPosition.y <= mEndPoint.y) {
                    // Checking startPoint and endPoint are in the same line or not
                    // if they are in same line , we need to check the chars that
                    // are appear between x axis
                    if (
                        mStartPoint.y >= line.relatedPosition.y && mStartPoint.y <= (line.relatedPosition.y + line.relatedSize.height) &&
                        mEndPoint.y >= line.relatedPosition.y && mEndPoint.y <= (line.relatedPosition.y + line.relatedSize.height)
                    ) {
                        val chars = arrayListOf<PdfChar>()
                        line.words.forEach { word -> chars.addAll(word.characters) }
                        val filteredCharsFirst = arrayListOf<PdfChar>()
                        chars.forEach { char ->
                            // if characters between startX and endX add the character in selected text
                            if (char.relatedPosition.x >= mStartPoint.x && char.relatedPosition.x <= mEndPoint.x) {
                                filteredCharsFirst.add(char)
                            }
                        }
                        if (filteredCharsFirst.isNotEmpty()) {
                            selectedLines.add(CharDrawSegments(filteredCharsFirst))
                        }
                    } else {
                        // if the code reach here , that means the selection is not single line

                        // checking current line is the first line or not
                        if (mStartPoint.y >= line.relatedPosition.y && mStartPoint.y <= (line.relatedPosition.y + line.relatedSize.height)) {
                            val chars = arrayListOf<PdfChar>()
                            line.words.forEach { word -> chars.addAll(word.characters) }
                            val filteredCharsFirst = arrayListOf<PdfChar>()
                            chars.forEach { char ->
                                // if current line is the first line , then adding characters those are-
                                // in x selection
                                if (char.relatedPosition.x >= mStartPoint.x) { filteredCharsFirst.add(char) }
                            }
                            if (filteredCharsFirst.isNotEmpty()) { selectedLines.add(
                                CharDrawSegments(filteredCharsFirst),
                            ) }
                        } else if (
                            // checking current line is the last line or not
                            mEndPoint.y >= line.relatedPosition.y && mEndPoint.y <= (line.relatedPosition.y + line.relatedSize.height)
                        ) {
                            val chars = arrayListOf<PdfChar>()
                            line.words.forEach { word -> chars.addAll(word.characters) }
                            val filteredCharsFirst = arrayListOf<PdfChar>()
                            chars.forEach { char ->
                                // if current line is the last line , then adding characters those are-
                                // in x selection
                                if (char.relatedPosition.x <= mEndPoint.x) { filteredCharsFirst.add(char) }
                            }
                            if (filteredCharsFirst.isNotEmpty()) {
                                selectedLines.add(CharDrawSegments(filteredCharsFirst))
                            }
                        } else {
                            // if the code reach here it means this line is between start and end line,
                            // so we don't need to check x values, so we just add them all in to selection
                            val chars = arrayListOf<PdfChar>()
                            line.words.forEach { word -> chars.addAll(word.characters) }
                            selectedLines.add(CharDrawSegments(chars))
                        }
                    }
                }
            }
        }
        textSelection.addLineSelection(selectedLines)
        redraw()
    }

    override fun onDraw(canvas: Canvas) {
        if (isInEditMode) {
            return
        }
        // As I said in this class javadoc, we can think of this canvas as a huge
        // strip on which we draw all the images. We actually only draw the rendered
        // parts, of course, but we render them in the place they belong in this huge
        // strip.

        // That's where Canvas.translate(x, y) becomes very helpful.
        // This is the situation :
        //  _______________________________________________
        // |   			 |					 			   |
        // | the actual  |					The big strip  |
        // |	canvas	 | 								   |
        // |_____________|								   |
        // |_______________________________________________|
        //
        // If the rendered part is on the bottom right corner of the strip
        // we can draw it but we won't see it because the canvas is not big enough.

        // But if we call translate(-X, -Y) on the canvas just before drawing the object :
        //  _______________________________________________
        // |   			  					  _____________|
        // |   The big strip     			 |			   |
        // |		    					 |	the actual |
        // |								 |	canvas	   |
        // |_________________________________|_____________|
        //
        // The object will be on the canvas.
        // This technique is massively used in this method, and allows
        // abstraction of the screen position when rendering the parts.

        // Draws background
        if (isAntialiasing) {
            canvas.drawFilter = antialiasFilter
        }
        val bg = background
        if (bg == null) {
            canvas.drawColor(if (nightMode) Color.BLACK else Color.WHITE)
        } else {
            bg.draw(canvas)
        }
        if (isRecycled) {
            return
        }
        if (state != State.SHOWN) {
            Log.e(TAG, "onDraw: cancelling drawing because state is $state")
            return
        }

        // Moves the canvas before drawing any element
        val currentXOffset = currentXOffset
        val currentYOffset = currentYOffset
        canvas.translate(currentXOffset, currentYOffset)

        // Draws thumbnails
        for (part in cacheManager?.thumbnails ?: emptyList()) {
            drawPart(canvas, part, true)
        }

        // Draws parts
        onDrawPagesNumbs.clear()
        val currentPagesPaginationIndexes = arrayListOf<Int>()
        val pageOffsets = arrayListOf<Float>()
        for (part in cacheManager?.pageParts ?: emptyList()) {
            drawPart(canvas, part)
            val page = part.page
            if (!onDrawPagesNumbs.contains(page)) {
                onDrawPagesNumbs.add(page)
                pageOffsets.add(pdfFile!!.getPageOffset(page))
                currentPagesPaginationIndexes.add(pdfFile!!.getPaginationIndexFromPageIndex(page))
            }
        }

        annotationHandler.drawAnnotations(currentPagesPaginationIndexes, pageOffsets, canvas, zoom)
        val selectionPage = textSelection.currentSelectionPageIndex
        if (selectionPage != -1) {
            val pageOffset = pdfFile!!.getPageOffset(selectionPage)
            textSelectionHelper.drawSelection(pageOffset, canvas, textSelection, zoom)
        }

        // Use the below function to debug annotation apply
//        annotationDebugDraw(canvas)

        // Restores the canvas position
        canvas.translate(-currentXOffset, -currentYOffset)
    }

    /**Use this function to debug annotation apply, you will also need to change the view width to
     * current pdf width then only you can debug the annotation
     * */
    private fun annotationDebugDraw(canvas: Canvas) {
        val pageOffset = pdfFile!!.getPageOffset(currentPage)

        pdfFile?.pageDetails?.getOrNull(currentPage)?.let { page ->
            page.coordinates.forEach { line ->
                line.words.forEach { word ->
                    word.characters.forEach { char ->

//                        canvas.drawLine(char.topPosition.x * zoom,(pageOffset + char.topPosition.y)*zoom,(char.topPosition.x+char.size.width)*zoom,(char.topPosition.y + pageOffset)*zoom,textSelectionHelper.strokeColorRed)
                        canvas.drawLine(char.bottomPosition.x * zoom, (pageOffset + (page.height - char.bottomPosition.y)) * zoom, (char.bottomPosition.x + char.size.width) * zoom, ((page.height - char.bottomPosition.y) + pageOffset) * zoom, textSelectionHelper.strokeColorRed)
                        val rect = RectF(char.bottomPosition.x, (pageOffset + (page.height - char.bottomPosition.y - char.size.height)), char.bottomPosition.x + char.size.width, (pageOffset + (page.height - char.bottomPosition.y)))
                        canvas.drawRect(rect.zoom(zoom), textSelectionHelper.strokeColorGreen)
                    }
                }
            }
        }

        annotationHandler.annotations.forEach {
            val page = pdfFile!!.getPageIndexFromPaginationIndex(it.paginationPageIndex)
            if (page == currentPage) {
                val height = pdfFile!!.getPageDetail(page).height
                val cord = when (it.type) {
                    PdfAnnotationModel.Type.Note -> it.asNote()?.coordinates
                    PdfAnnotationModel.Type.Highlight -> it.asHighlight()?.coordinates
                }
                if (cord != null) {
                    val rect = RectF(
                        cord.startX.toFloat(),
                        pageOffset + (height - cord.startY.toFloat()),
                        cord.endX.toFloat(),
                        pageOffset + (height - cord.endY.toFloat()),
                    )
                    canvas.drawRect(
                        rect.zoom(zoom),
                        textSelectionHelper.strokeColorRed.apply {
                            this.strokeWidth = 1f * zoom
                        },
                    )
                }
            }
        }
    }

    /** Draw a given PagePart on the canvas  */
    private fun drawPart(canvas: Canvas, part: PagePart, isCache: Boolean = false) {
        // Can seem strange, but avoid lot of calls
        val pageRelativeBounds = part.pageRelativeBounds
        val renderedBitmap = part.renderedBitmap
        if (renderedBitmap.isRecycled) {
            return
        }

        // Move to the target page
        val localTranslationX: Float
        val localTranslationY: Float
        val size = pdfFile!!.getPageSize(part.page)
        if (isSwipeVertical) {
            localTranslationY = pdfFile!!.getPageOffsetWithZoom(part.page, zoom)
            if (!isCache && part.page != 0) {
                // Drawing Line separator between pages
                canvas.drawLine(
                    0f,
                    localTranslationY,
                    toCurrentScale(width.toFloat()),
                    localTranslationY,
                    pageDivisionPaint,
                )
            }
            val maxWidth = pdfFile!!.maxPageWidth
            localTranslationX = toCurrentScale(maxWidth - size.width) / 2
        } else {
            localTranslationX = pdfFile!!.getPageOffsetWithZoom(part.page, zoom)
            val maxHeight = pdfFile!!.maxPageHeight
            localTranslationY = toCurrentScale(maxHeight - size.height) / 2
        }
        canvas.translate(localTranslationX, localTranslationY)
        val srcRect = Rect(
            0,
            0,
            renderedBitmap.width,
            renderedBitmap.height,
        )
        val offsetX = toCurrentScale(pageRelativeBounds.left * size.width)
        val offsetY = toCurrentScale(pageRelativeBounds.top * size.height)
        val width = toCurrentScale(pageRelativeBounds.width() * size.width)
        val height = toCurrentScale(pageRelativeBounds.height() * size.height)
        // If we use float values for this rectangle, there will be
        // a possible gap between page parts, especially when
        // the zoom level is high.
        val dstRect = RectF(
            offsetX.toInt().toFloat(),
            offsetY.toInt().toFloat(),
            (offsetX + width).toInt().toFloat(),
            (offsetY + height).toInt().toFloat(),
        )

        // Check if bitmap is in the screen
        val translationX = currentXOffset + localTranslationX
        val translationY = currentYOffset + localTranslationY
        if (translationX + dstRect.left >= getWidth() || translationX + dstRect.right <= 0 || translationY + dstRect.top >= getHeight() || translationY + dstRect.bottom <= 0) {
            canvas.translate(-localTranslationX, -localTranslationY)
            return
        }
        canvas.drawBitmap(renderedBitmap, srcRect, dstRect, paint)
        if (Constants.DEBUG_MODE) {
            debugPaint.color = if (part.page % 2 == 0) Color.RED else Color.BLUE
            canvas.drawRect(dstRect, debugPaint)
        }

        // Restore the canvas position
        canvas.translate(-localTranslationX, -localTranslationY)
    }

    /**
     * Load all the parts around the center of the screen,
     * taking into account X and Y offsets, zoom level, and
     * the current page displayed
     */
    fun loadPages() {
        if (pdfFile == null || renderingHandler == null) {
            return
        }

        // Cancel all current tasks
        renderingHandler!!.removeMessages(RenderingHandler.MSG_RENDER_TASK)
        cacheManager?.makeANewSet()
        pagesLoader?.loadPages()
        redraw()
    }

    /** Called when the PDF is loaded  */
    fun loadComplete(pdfFile: PdfFile) {
        scope?.launch(Dispatchers.IO) {
            try {
                state = State.LOADED

                this@PDFView.pdfFile = pdfFile
                this@PDFView.pdfFile?.setMergeListener(this@PDFView)
                this@PDFView.pdfFile?.prepareCharsRelativeSize()

                startRendering()
                dragPinchManager?.enable()
                callbacks.callOnLoadComplete(pdfFile.pagesCount)
                withContext(Dispatchers.Main) {
                    jumpToWithPaginationPageIndex(defaultPage, false)
                    listener?.onPreparationSuccess()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    listener?.onPreparationFailed(e.message ?: "Something Went wrong", e)
                }
            }
        }
    }

    private fun startRendering() {
        if (!renderingHandlerThread!!.isAlive) {
            renderingHandlerThread!!.start()
        }
        renderingHandler = RenderingHandler(
            renderingHandlerThread!!.looper,
            this@PDFView,
        )
        renderingHandler!!.start()
        if (scrollHandle != null) {
            scrollHandle!!.setupLayout(this@PDFView)
            isScrollHandleInit = true
        }
    }

    fun onMergedPdfLoaded(
        requestCode: Int,
        mergeId: Int,
        document: PdfDocument,
        startPage: Int,
        pageDetails: ArrayList<PageModel>,
        pageToLoad: Int,
    ) {
        if (mergeId != currentMergeId) return
        when (requestCode) {
            MERGE_TOP_PDF_REQUEST_CODE -> pdfFile?.mergeDataAtTop(mergeId, document, startPage, pageDetails, pageToLoad)
            MERGE_BOTTOM_PDF_REQUEST_CODE -> pdfFile?.mergeDataAtBottom(mergeId, document, startPage, pageDetails, pageToLoad)
        }
    }

    fun loadError(t: Exception?) {
        state = State.ERROR
        // store reference, because callbacks will be cleared in recycle() method
        val onErrorListener = callbacks.onError
        recycle()
        invalidate()
        onErrorListener?.onError(t) ?: Log.e("PDFView", "load pdf error", t)
        listener?.onPreparationFailed(t?.message ?: "Something went wrong", t)
    }
    fun mergeLoadError(requestCode: Int, mergeId: Int, t: Exception?) {
        val mergeType = when (requestCode) {
            MERGE_TOP_PDF_REQUEST_CODE -> PdfFile.MergeType.TOP
            MERGE_BOTTOM_PDF_REQUEST_CODE -> PdfFile.MergeType.BOTTOM
            else -> PdfFile.MergeType.BOTTOM
        }
        mergeState = MergeState.IDLE
        listener?.onMergeFailed(mergeId, mergeType, t?.message ?: "Something went wrong", t)
    }

    fun redraw() {
        invalidate()
    }

    /**
     * Called when a rendering task is over and
     * a PagePart has been freshly created.
     *
     * @param part The created PagePart.
     */
    fun onBitmapRendered(part: PagePart) {
        // when it is first rendered part
        if (state == State.LOADED) {
            state = State.SHOWN
            callbacks.callOnRender(pdfFile!!.pagesCount)
        }
        if (part.isThumbnail) {
            cacheManager?.cacheThumbnail(part)
        } else {
            cacheManager?.cachePart(part)
        }
        redraw()
    }

    private var bottomEndReached = false
    private var topEndReached = false

    /**
     * Move to the given X and Y offsets, but check them ahead of time
     * to be sure not to go outside the the big strip.
     *
     * @param offsetX    The big strip X offset to use as the left border of the screen.
     * @param offsetY    The big strip Y offset to use as the right border of the screen.
     * @param moveHandle whether to move scroll handle or not
     */
    @JvmOverloads
    fun moveTo(offsetX: Float, offsetY: Float, moveHandle: Boolean = true, fromScrolling: Boolean = false) {
        var mOffsetX = offsetX
        var mOffsetY = offsetY
        var maxScrollReached = false
        if (isSwipeVertical) {
            // Check X offset
            val scaledPageWidth = toCurrentScale(pdfFile!!.maxPageWidth)
            if (scaledPageWidth < width) {
                mOffsetX = width / 2 - scaledPageWidth / 2
            } else {
                if (mOffsetX > 0) {
                    mOffsetX = 0f
                } else if (mOffsetX + scaledPageWidth < width) {
                    mOffsetX = width - scaledPageWidth
                }
            }

            // Check Y offset
            val contentHeight = pdfFile!!.getDocLen(zoom)
            if (contentHeight < height) { // whole document height visible on screen
                mOffsetY = (height - contentHeight) / 2
            } else {
                if (mOffsetY > 0) { // top visible
                    mOffsetY = 0f
                    if (!topEndReached) {
                        onMaximumTopReach()
                    }
                    topEndReached = true
                    maxScrollReached = true
                } else if (mOffsetY + contentHeight < height) { // bottom visible
                    mOffsetY = -contentHeight + height
                    if (!bottomEndReached) {
                        onMaximumBottomReach()
                    }
                    bottomEndReached = true
                    maxScrollReached = true
                } else {
                    bottomEndReached = false
                    topEndReached = false
                }
            }
            scrollDir = if (mOffsetY < currentYOffset) {
                ScrollDir.END
            } else if (mOffsetY > currentYOffset) {
                ScrollDir.START
            } else {
                ScrollDir.NONE
            }
        } else {
            // Check Y offset
            val scaledPageHeight = toCurrentScale(pdfFile!!.maxPageHeight)
            if (scaledPageHeight < height) {
                mOffsetY = height / 2 - scaledPageHeight / 2
            } else {
                if (mOffsetY > 0) {
                    mOffsetY = 0f
                } else if (mOffsetY + scaledPageHeight < height) {
                    mOffsetY = height - scaledPageHeight
                }
            }

            // Check X offset
            val contentWidth = pdfFile!!.getDocLen(zoom)
            if (contentWidth < width) { // whole document width visible on screen
                mOffsetX = (width - contentWidth) / 2
            } else {
                if (mOffsetX > 0) { // left visible
                    mOffsetX = 0f
                } else if (mOffsetX + contentWidth < width) { // right visible
                    mOffsetX = -contentWidth + width
                }
            }
            scrollDir = if (mOffsetX < currentXOffset) {
                ScrollDir.END
            } else if (mOffsetX > currentXOffset) {
                ScrollDir.START
            } else {
                ScrollDir.NONE
            }
        }
        currentXOffset = mOffsetX
        currentYOffset = mOffsetY
        val positionOffset = positionOffset
        if (moveHandle && scrollHandle != null && !documentFitsView()) {
            scrollHandle!!.setScroll(positionOffset)
        }
        callbacks.callOnPageScroll(currentPage, positionOffset)
        if (!maxScrollReached && fromScrolling) {
            listener?.onScrolling()
        }
        redraw()
    }

    private fun onMaximumTopReach() {
        if (mergeState != MergeState.IDLE) {
            Log.e(TAG, "Top Pagination cancelled, already a merging working")
            return
        }
        val pageToLoad = (pdfFile?.paginationStartPageIndex ?: 0) - 1
        if (pageToLoad >= 0) {
            listener?.loadTopPdfChunk(getMergeId(), pageToLoad)
            mergeState = MergeState.TOP_MERGING_LOADING
        }
    }
    private fun onMaximumBottomReach() {
        if (mergeState != MergeState.IDLE) {
            Log.e(TAG, "Bottom Pagination cancelled, already a merging working")
            return
        }

        if (totalPdfPage == -1) {
            Log.e(TAG, "Failed to paginate pdf, you haven't set pdf total page")
            return
        }
        val pageToLoad = pdfFile!!.paginationEndPageIndex + 1
        if (pageToLoad < totalPdfPage) {
            listener?.loadBottomPdfChunk(getMergeId(), pageToLoad)
            mergeState = MergeState.BOTTOM_MERGE_LOADING
        } else {
            Log.e(TAG, "Maximum pagination limit is reached")
        }
    }

    fun loadPageByOffset() {
        if (0 == pdfFile!!.pagesCount) {
            return
        }
        val offset: Float
        val screenCenter: Float
        if (isSwipeVertical) {
            offset = currentYOffset
            screenCenter = height.toFloat() / 2
        } else {
            offset = currentXOffset
            screenCenter = width.toFloat() / 2
        }
        val page = pdfFile!!.getPageAtOffset(-(offset - screenCenter), zoom)
        if (page >= 0 && page <= pdfFile!!.pagesCount - 1 && page != currentPage) {
            showPage(page)
        } else {
            loadPages()
        }
    }

    /**
     * Animate to the nearest snapping position for the current SnapPolicy
     */
    fun performPageSnap() {
        if (!isPageSnap || pdfFile == null || pdfFile!!.pagesCount == 0) {
            return
        }
        val centerPage = findFocusPage(currentXOffset, currentYOffset)
        val edge = findSnapEdge(centerPage)
        if (edge == SnapEdge.NONE) {
            return
        }
        val offset = snapOffsetForPage(centerPage, edge)
        if (isSwipeVertical) {
            animationManager?.startYAnimation(currentYOffset, -offset)
        } else {
            animationManager?.startXAnimation(currentXOffset, -offset)
        }
    }

    /**
     * Find the edge to snap to when showing the specified page
     */
    fun findSnapEdge(page: Int): SnapEdge {
        if (!isPageSnap || page < 0) {
            return SnapEdge.NONE
        }
        val currentOffset = if (isSwipeVertical) currentYOffset else currentXOffset
        val offset = -pdfFile!!.getPageOffsetWithZoom(page, zoom)
        val length = if (isSwipeVertical) height else width
        val pageLength = pdfFile!!.getPageLength(page, zoom)
        return if (length >= pageLength) {
            SnapEdge.CENTER
        } else if (currentOffset >= offset) {
            SnapEdge.START
        } else if (offset - pageLength > currentOffset - length) {
            SnapEdge.END
        } else {
            SnapEdge.NONE
        }
    }

    /**
     * Get the offset to move to in order to snap to the page
     */
    fun snapOffsetForPage(pageIndex: Int, edge: SnapEdge): Float {
        var offset = pdfFile!!.getPageOffsetWithZoom(pageIndex, zoom)
        val length = if (isSwipeVertical) height.toFloat() else width.toFloat()
        val pageLength = pdfFile!!.getPageLength(pageIndex, zoom)
        if (edge == SnapEdge.CENTER) {
            offset = offset - length / 2f + pageLength / 2f
        } else if (edge == SnapEdge.END) {
            offset = offset - length + pageLength
        }
        return offset
    }

    fun findFocusPage(xOffset: Float, yOffset: Float): Int {
        val currOffset = if (isSwipeVertical) yOffset else xOffset
        val length = if (isSwipeVertical) height.toFloat() else width.toFloat()
        // make sure first and last page can be found
        if (currOffset > -1) {
            return 0
        } else if (currOffset < -pdfFile!!.getDocLen(zoom) + length + 1) {
            return pdfFile!!.pagesCount - 1
        }
        // else find page in center
        val center = currOffset - length / 2f
        return pdfFile!!.getPageAtOffset(-center, zoom)
    }

    fun scrollWithAnimation(scrollBy: Int) {
        animationManager?.startYAnimation(currentYOffset, currentYOffset + scrollBy)
    }

    /**
     * @return true if single page fills the entire screen in the scrolling direction
     */
    fun pageFillsScreen(): Boolean {
        val start = -pdfFile!!.getPageOffsetWithZoom(currentPage, zoom)
        val end = start - pdfFile!!.getPageLength(currentPage, zoom)
        return if (isSwipeVertical) {
            start > currentYOffset && end < currentYOffset - height
        } else {
            start > currentXOffset && end < currentXOffset - width
        }
    }

    /**
     * Move relatively to the current position.
     *
     * @param dx The X difference you want to apply.
     * @param dy The Y difference you want to apply.
     * @see .moveTo
     */
    fun moveRelativeTo(dx: Float, dy: Float) {
        moveTo(currentXOffset + dx, currentYOffset + dy, fromScrolling = true)
    }

    /**
     * Change the zoom level
     */
    fun zoomTo(zoom: Float) {
        this.zoom = zoom
    }

    /**
     * Change the zoom level, relatively to a pivot point.
     * It will call moveTo() to make sure the given point stays
     * in the middle of the screen.
     *
     * @param zoom  The zoom level.
     * @param pivot The point on the screen that should stays.
     */
    fun zoomCenteredTo(zoom: Float, pivot: PointF) {
        val dZoom = zoom / this.zoom
        zoomTo(zoom)
        var baseX = currentXOffset * dZoom
        var baseY = currentYOffset * dZoom
        baseX += pivot.x - pivot.x * dZoom
        baseY += pivot.y - pivot.y * dZoom
        moveTo(baseX, baseY)
    }

    /**
     * @see .zoomCenteredTo
     */
    fun zoomCenteredRelativeTo(dZoom: Float, pivot: PointF) {
        zoomCenteredTo(zoom * dZoom, pivot)
    }

    /**
     * Checks if whole document can be displayed on screen, doesn't include zoom
     *
     * @return true if whole document can displayed at once, false otherwise
     */
    fun documentFitsView(): Boolean {
        val len = pdfFile!!.getDocLen(1f)
        return if (isSwipeVertical) {
            len < height
        } else {
            len < width
        }
    }

    fun fitToWidth(page: Int) {
        if (state != State.SHOWN) {
            Log.e(TAG, "Cannot fit, document not rendered yet")
            return
        }
        zoomTo(width / pdfFile!!.getPageSize(page).width)
        jumpTo(page)
    }

    fun getPageSize(pageIndex: Int): SizeF {
        return if (pdfFile == null) {
            SizeF(0f, 0f)
        } else {
            pdfFile!!.getPageSize(pageIndex)
        }
    }

    fun toRealScale(size: Float): Float {
        return size / zoom
    }

    fun toCurrentScale(size: Float): Float {
        return size * zoom
    }

    private fun Float.scaledX(): Float {
        return this - currentXOffset
    }
    private fun Float.scaledY(): Float {
        return this - currentYOffset
    }

    val isZooming: Boolean
        get() = zoom != minZoom

    private fun setDefaultPage(defaultPage: Int) {
        this.defaultPage = defaultPage
    }

    fun resetZoom() {
        zoomTo(minZoom)
    }

    fun resetZoomWithAnimation() {
        zoomWithAnimation(minZoom)
    }

    fun zoomWithAnimation(centerX: Float, centerY: Float, scale: Float) {
        animationManager?.startZoomAnimation(centerX, centerY, zoom, scale)
    }

    fun zoomWithAnimation(scale: Float, onAnimationEnd: (() -> Unit)? = null) {
        animationManager?.startZoomAnimation(
            (width / 2).toFloat(),
            (height / 2).toFloat(),
            zoom,
            scale,
            onAnimationEnd,
        )
    }

    /**
     * Get page number at given offset
     *
     * @param positionOffset scroll offset between 0 and 1
     * @return page number at given offset, starting from 0
     */
    fun getPageAtPositionOffset(positionOffset: Float): Int {
        return pdfFile!!.getPageAtOffset(pdfFile!!.getDocLen(zoom) * positionOffset, zoom)
    }

    fun useBestQuality(bestQuality: Boolean) {
        isBestQuality = bestQuality
    }

    fun enableAnnotationRendering(annotationRendering: Boolean) {
        isAnnotationRendering = annotationRendering
    }

    fun enableRenderDuringScale(renderDuringScale: Boolean) {
        this.renderDuringScale = renderDuringScale
    }

    fun enableAntialiasing(enableAntialiasing: Boolean) {
        isAntialiasing = enableAntialiasing
    }

    fun setPageFling(pageFling: Boolean) {
        isPageFlingEnabled = pageFling
    }

    private fun setSpacing(spacingDp: Int) {
        spacingPx = Util.getDP(context, spacingDp)
    }

    private fun setAutoSpacing(autoSpacing: Boolean) {
        isAutoSpacingEnabled = autoSpacing
    }

    fun doRenderDuringScale(): Boolean {
        return renderDuringScale
    }

    /** Returns null if document is not loaded  */
    val documentMeta: Meta?
        get() = if (pdfFile == null) {
            null
        } else {
            pdfFile!!.metaData
        }

    /** Will be empty until document is loaded  */
    val tableOfContents: List<Bookmark>
        get() = if (pdfFile == null) {
            emptyList()
        } else {
            pdfFile!!.bookmarks
        }

    /** Will be empty until document is loaded  */
    fun getLinks(page: Int): List<PdfDocument.Link> {
        return if (pdfFile == null) {
            emptyList()
        } else {
            pdfFile!!.getPageLinks(page)
        }
    }

//    /** Use an asset file as the pdf source  */
//    fun fromAsset(assetName: String?): Configurator {
//        return Configurator(AssetSource(assetName))
//    }
//
//    /** Use a file as the pdf source  */
    fun fromFile(file: File, startPageIndex: Int = 0): Configurator {
        this.file = file
        return Configurator(FileSource(file, startPageIndex))
    }
//
//    /** Use URI as the pdf source, for use with content providers  */
//    fun fromUri(uri: Uri?): Configurator {
//        return Configurator(UriSource(uri))
//    }

    /** Use bytearray as the pdf source, documents is not saved  */
    fun fromBytes(bytes: ByteArray): Configurator {
        return Configurator(ByteArraySource(bytes))
    }
//
//    /** Use stream as the pdf source. Stream will be written to bytearray, because native code does not support Java Streams  */
//    fun fromStream(stream: InputStream?): Configurator {
//        return Configurator(InputStreamSource(stream)!!)
//    }
//
//    /** Use custom source as pdf source  */
//    fun fromSource(docSource: DocumentSource?): Configurator {
//        return Configurator(docSource!!)
//    }

    private enum class State {
        DEFAULT, LOADED, SHOWN, ERROR
    }

    inner class Configurator constructor(private val documentSource: DocumentSource) {
        private var pageNumbers: IntArray? = null
        private var enableSwipe = true
        private var enableDoubleTap = true
        private var onDrawListener: OnDrawListener? = null
        private var onDrawAllListener: OnDrawListener? = null
        private var onLoadCompleteListener: OnLoadCompleteListener? = null
        private var onErrorListener: OnErrorListener? = null
        private var onPageChangeListener: OnPageChangeListener? = null
        private var onPageScrollListener: OnPageScrollListener? = null
        private var onRenderListener: OnRenderListener? = null
        private var onTapListener: OnTapListener? = null
        private var onLongPressListener: OnLongPressListener? = null
        private var onPageErrorListener: OnPageErrorListener? = null
        private var linkHandler: LinkHandler = DefaultLinkHandler(this@PDFView)
        private var defaultPage = 0
        private var swipeHorizontal = false
        private var annotationRendering = false
        private var password: String? = null
        private var scrollHandle: ScrollHandle? = null
        private var antialiasing = true
        private var spacing = 0
        private var autoSpacing = false
        private var pageFitPolicy = FitPolicy.WIDTH
        private var fitEachPage = false
        private var pageFling = false
        private var pageSnap = false
        private var nightMode = false
        private var file: File? = null
        private var startPage: Int = 1
        private var paginationDefaultPageIndex = -1
        fun pages(vararg pageNumbers: Int): Configurator {
            this.pageNumbers = pageNumbers
            return this
        }

        fun enableSwipe(enableSwipe: Boolean): Configurator {
            this.enableSwipe = enableSwipe
            return this
        }

        fun enableDoubleTap(enableDoubleTap: Boolean): Configurator {
            this.enableDoubleTap = enableDoubleTap
            return this
        }

        fun enableAnnotationRendering(annotationRendering: Boolean): Configurator {
            this.annotationRendering = annotationRendering
            return this
        }

        fun onDraw(onDrawListener: OnDrawListener?): Configurator {
            this.onDrawListener = onDrawListener
            return this
        }

        fun onDrawAll(onDrawAllListener: OnDrawListener?): Configurator {
            this.onDrawAllListener = onDrawAllListener
            return this
        }

        fun onLoad(onLoadCompleteListener: OnLoadCompleteListener?): Configurator {
            this.onLoadCompleteListener = onLoadCompleteListener
            return this
        }

        fun onPageScroll(onPageScrollListener: OnPageScrollListener?): Configurator {
            this.onPageScrollListener = onPageScrollListener
            return this
        }

        fun onError(onErrorListener: OnErrorListener?): Configurator {
            this.onErrorListener = onErrorListener
            return this
        }

        fun onPageError(onPageErrorListener: OnPageErrorListener?): Configurator {
            this.onPageErrorListener = onPageErrorListener
            return this
        }

        fun onPageChange(onPageChangeListener: OnPageChangeListener?): Configurator {
            this.onPageChangeListener = onPageChangeListener
            return this
        }

        fun onRender(onRenderListener: OnRenderListener?): Configurator {
            this.onRenderListener = onRenderListener
            return this
        }

        fun onTap(onTapListener: OnTapListener?): Configurator {
            this.onTapListener = onTapListener
            return this
        }

        fun onLongPress(onLongPressListener: OnLongPressListener?): Configurator {
            this.onLongPressListener = onLongPressListener
            return this
        }

        fun linkHandler(linkHandler: LinkHandler): Configurator {
            this.linkHandler = linkHandler
            return this
        }

        fun defaultPage(defaultPage: Int): Configurator {
            this.defaultPage = defaultPage
            return this
        }

        fun swipeHorizontal(swipeHorizontal: Boolean): Configurator {
            this.swipeHorizontal = swipeHorizontal
            return this
        }

        fun password(password: String?): Configurator {
            this.password = password
            return this
        }

        fun scrollHandle(scrollHandle: ScrollHandle?): Configurator {
            this.scrollHandle = scrollHandle
            return this
        }

        fun enableAntialiasing(antialiasing: Boolean): Configurator {
            this.antialiasing = antialiasing
            return this
        }

        fun spacing(spacing: Int): Configurator {
            this.spacing = spacing
            return this
        }

        fun autoSpacing(autoSpacing: Boolean): Configurator {
            this.autoSpacing = autoSpacing
            return this
        }

        fun pageFitPolicy(pageFitPolicy: FitPolicy): Configurator {
            this.pageFitPolicy = pageFitPolicy
            return this
        }

        fun fitEachPage(fitEachPage: Boolean): Configurator {
            this.fitEachPage = fitEachPage
            return this
        }

        fun pageSnap(pageSnap: Boolean): Configurator {
            this.pageSnap = pageSnap
            return this
        }

        fun pageFling(pageFling: Boolean): Configurator {
            this.pageFling = pageFling
            return this
        }

        fun nightMode(nightMode: Boolean): Configurator {
            this.nightMode = nightMode
            return this
        }

        fun disableLongPress(): Configurator {
            dragPinchManager?.disableLongpress()
            return this
        }

        fun setPdfStartPage(page: Int): Configurator {
            this.startPage = page
            return this
        }

        /**This page will be opened after loading pdf chunks, note that this page number should be related to whole pdf document, not related to pdf chunk*/
        fun pdfDefaultPage(paginationPageIndex: Int): Configurator {
            paginationDefaultPageIndex = paginationPageIndex
            return this
        }

        fun load() {
            if (!hasSize) {
                waitingDocumentConfigurator = this
                return
            }
            recycle()
            callbacks.setOnLoadComplete(onLoadCompleteListener)
            callbacks.onError = onErrorListener
            callbacks.onDraw = onDrawListener
            callbacks.onDrawAll = onDrawAllListener
            callbacks.setOnPageChange(onPageChangeListener)
            callbacks.setOnPageScroll(onPageScrollListener)
            callbacks.setOnRender(onRenderListener)
            callbacks.setOnTap(onTapListener)
            callbacks.setOnLongPress(onLongPressListener)
            callbacks.setOnPageError(onPageErrorListener)
            callbacks.setLinkHandler(linkHandler)
            isSwipeEnabled = enableSwipe
            setNightMode(nightMode)
            this@PDFView.enableDoubleTap(enableDoubleTap)
            setDefaultPage(defaultPage)
            isSwipeVertical = !swipeHorizontal
            this@PDFView.enableAnnotationRendering(annotationRendering)
            this@PDFView.scrollHandle = scrollHandle
            this@PDFView.enableAntialiasing(antialiasing)
            setSpacing(spacing)
            setAutoSpacing(autoSpacing)
            this@PDFView.pageFitPolicy = pageFitPolicy
            isFitEachPage = fitEachPage
            isPageSnap = pageSnap
            setPageFling(pageFling)
//            if (pageNumbers != null) {
//                this@PDFView.load(documentSource, password, pageNumbers, byteArray)
//            } else {
            this@PDFView.load(documentSource, password)
//            }
        }
    }

    companion object {
        private val TAG = PDFView::class.java.simpleName
        const val DEFAULT_MAX_SCALE = 5.0f
        const val DEFAULT_MID_SCALE = 1.75f
        const val DEFAULT_MIN_SCALE = 1.0f

        const val MERGE_TOP_PDF_REQUEST_CODE = 1111
        const val MERGE_BOTTOM_PDF_REQUEST_CODE = 2222
    }

    /**Clear text selection and the coordinates used to find textSelection*/
    fun clearAllTextSelectionAndCoordinates() {
        textSelection.clearAllSelection(true)
        textSelectionHelper.endSelectionPosition.set(0f, 0f)
        textSelectionHelper.endHandlePosition.set(0f, 0f)
        textSelectionHelper.startSelectionPosition.set(0f, 0f)
        textSelectionHelper.startHandlePosition.set(0f, 0f)
        listener?.onTextSelectionCleared()
        redraw()
    }
    fun clearAllTextSelection(redraw: Boolean = true) {
        textSelection.clearAllSelection()
        listener?.onTextSelectionCleared()
        if (redraw) {
            redraw()
        }
    }

    fun addHighlight(highlight: HighlightModel) {
        val coordinates = highlight.coordinates ?: return
        val startPoint = PointF(coordinates.startX.toFloat(), coordinates.startY.toFloat())
        val endPoint = PointF(coordinates.endX.toFloat(), coordinates.endY.toFloat())
        val drawSegments = findDrawSegmentsOfAnnotation(pdfFile!!.getPageIndexFromPaginationIndex(highlight.paginationPageIndex), startPoint, endPoint)
        highlight.charDrawSegments = drawSegments
        annotationHandler.annotations.add(highlight)
        redraw()
    }

    fun removeHighlightAnnotations(highlightIds: List<Int>) {
        annotationHandler.removeHighlightAnnotation(highlightIds)
        redraw()
    }
    fun addComment(note: CommentModel) {
        val coordinates = note.coordinates ?: return
        val startPoint = PointF(coordinates.startX.toFloat(), coordinates.startY.toFloat())
        val endPoint = PointF(coordinates.endX.toFloat(), coordinates.endY.toFloat())
        val drawSegments = findDrawSegmentsOfAnnotation(pdfFile!!.getPageIndexFromPaginationIndex(note.paginationPageIndex), startPoint, endPoint)
        note.charDrawSegments = drawSegments
        annotationHandler.annotations.add(note)
        redraw()
    }

    fun removeNoteAnnotations(noteIds: List<Int>) {
        annotationHandler.removeNoteAnnotation(noteIds)
        redraw()
    }

    private fun findDrawSegmentsOfAnnotation(
        pageIndex: Int,
        startPoint: PointF,
        endPoint: PointF,
    ): ArrayList<CharDrawSegments> {
        val segments = arrayListOf<CharDrawSegments>()
        pdfFile?.pageDetails?.getOrNull(pageIndex)?.let {
            it.coordinates.forEach { line ->
                // extracting characters inside the selection points
                val bottomYStart = it.height - startPoint.y
                val bottomYEnd = it.height - endPoint.y
                val newStartPoint = PointF(startPoint.x, bottomYStart)
                val newEndPoint = PointF(endPoint.x, bottomYEnd)
                val drawSegments = getDrawSegmentInPoints(line, newStartPoint, newEndPoint)
                if (drawSegments != null) {
                    segments.add(drawSegments)
                }
            }
        }
        return segments
    }

    private fun getDrawSegmentInPoints(
        line: PdfLine,
        startPoint: PointF,
        endPoint: PointF,
    ): CharDrawSegments? {
        // extracting characters inside the selection points
        // Checking all the lines that between start and end point
        if ((line.position.y + line.size.height) >= startPoint.y && line.position.y <= endPoint.y) {
            // Checking startPoint and endPoint are in the same line or not
            // if they are in same line , we need to check the chars that
            // are appear between x axis
            if (
                startPoint.y >= line.position.y && startPoint.y <= (line.position.y + line.size.height) &&
                endPoint.y >= line.position.y && endPoint.y <= (line.position.y + line.size.height)
            ) {
                val chars = arrayListOf<PdfChar>()
                line.words.forEach { word -> chars.addAll(word.characters) }
                val filteredCharsFirst = arrayListOf<PdfChar>()
                chars.forEach { char ->
                    // if characters between startX and endX add the character in selected text
                    if ((char.topPosition.x + char.size.width / 2) >= startPoint.x && (char.topPosition.x + char.size.width / 2) <= endPoint.x) {
                        filteredCharsFirst.add(char)
                    }
                }
                if (filteredCharsFirst.isNotEmpty()) {
                    return CharDrawSegments(filteredCharsFirst)
                }
            } else {
                // if the code reach here , that means the selection is not single line

                // checking current line is the first line or not
                if (startPoint.y >= line.position.y && startPoint.y <= (line.position.y + line.size.height)) {
                    val chars = arrayListOf<PdfChar>()
                    line.words.forEach { word -> chars.addAll(word.characters) }
                    val filteredCharsFirst = arrayListOf<PdfChar>()
                    chars.forEach { char ->
                        // if current line is the first line , then adding characters those are-
                        // in x selection
                        if ((char.topPosition.x + char.size.width / 2) >= startPoint.x) {
                            filteredCharsFirst.add(char)
                        }
                    }
                    if (filteredCharsFirst.isNotEmpty()) {
                        return CharDrawSegments(filteredCharsFirst)
                    }
                } else if (
                    // checking current line is the last line or not
                    endPoint.y >= line.position.y && endPoint.y <= (line.position.y + line.size.height)
                ) {
                    val chars = arrayListOf<PdfChar>()
                    line.words.forEach { word -> chars.addAll(word.characters) }
                    val filteredCharsFirst = arrayListOf<PdfChar>()
                    chars.forEach { char ->
                        // if current line is the last line , then adding characters those are-
                        // in x selection
                        if (char.topPosition.x + char.size.width / 2 <= endPoint.x) {
                            filteredCharsFirst.add(char)
                        }
                    }
                    if (filteredCharsFirst.isNotEmpty()) {
                        return CharDrawSegments(filteredCharsFirst)
                    }
                } else {
                    // if the code reach here it means this line is between start and end line,
                    // so we don't need to check x values, so we just add them all in to selection
                    val chars = arrayListOf<PdfChar>()
                    line.words.forEach { word -> chars.addAll(word.characters) }
                    return CharDrawSegments(chars)
                }
            }
        }
        return null
    }


    override fun mergeStart(mergeType: PdfFile.MergeType) {
    }

    override fun mergeEnd(
        mergeId: Int,
        mergeType: PdfFile.MergeType,
        mergedFileDocLength: Float,
        pageToLoad: Int,
    ) {
        // Update selection page , after pagination page indexes may change!
        if (textSelection.currentSelectionPageIndex != -1 && textSelection.currentSelectionPaginationIndex != -1) {
            textSelection.currentSelectionPageIndex = pdfFile?.getPageIndexFromPaginationIndex(textSelection.currentSelectionPaginationIndex) ?: -1
        }

        // if pages are added from the top, then we need to to update currentYOffset accordingly to view the same page the user have now
        if (mergeType == PdfFile.MergeType.TOP) {
            cacheManager?.recycle()
            currentYOffset -= mergedFileDocLength * zoom
        }
        loadPages()
        // giving callbacks to reader activity to dismiss loader also changing merging state
        mergeState = MergeState.IDLE
        listener?.onMergeEnd(mergeId, mergeType)
    }

    fun resetZoomAndXOffset() {
        currentXOffset = 0f
        zoom = 1f
        redraw()
    }

    fun getCurrentVisiblePaginationPageIndexes(): List<Int> {
        if (pdfFile != null) {
            return onDrawPagesNumbs.map { pdfFile!!.getPaginationIndexFromPageIndex(it) }
        }
        return emptyList()
    }

    fun getPaginationStartPageIndex(): Int {
        return pdfFile?.paginationStartPageIndex ?: 0
    }
    fun getPaginationEndPageIndex(): Int {
        return pdfFile?.paginationEndPageIndex ?: 0
    }

    /**This index is not based the current pdf chunk, it is the index of page in the whole pdf doc*/
    fun getCurrentPdfPage(): Int {
        return pdfFile?.getPaginationIndexFromPageIndex(currentPage) ?: -1
    }

    fun loadAnnotations(annotations: List<PdfAnnotationModel>) {
        annotationHandler.clearAllAnnotations()
        scope?.launch(Dispatchers.IO) {
            for (annotation in annotations) {
                val coordinates = when (annotation.type) {
                    PdfAnnotationModel.Type.Note -> annotation.asNote()?.updateAnnotationData()?.coordinates
                    PdfAnnotationModel.Type.Highlight -> annotation.asHighlight()?.updateAnnotationData()?.coordinates
                }
                coordinates ?: continue

                val startPoint = PointF(coordinates.startX.toFloat(), coordinates.startY.toFloat())
                val endPoint = PointF(coordinates.endX.toFloat(), coordinates.endY.toFloat())
                val drawSegments = findDrawSegmentsOfAnnotation(pdfFile!!.getPageIndexFromPaginationIndex(annotation.paginationPageIndex), startPoint, endPoint)
                annotation.charDrawSegments = drawSegments
            }
            withContext(Dispatchers.Main) {
                annotationHandler.annotations.addAll(annotations)
                redraw()
            }
        }
    }

    fun updateMergeFailed() {
        mergeState = MergeState.IDLE
    }

    interface Listener {
        fun onPreparationStarted()
        fun onPreparationSuccess()
        fun onPreparationFailed(error: String, e: Exception?)

        fun onPageChanged(pageIndex: Int, paginationPageIndex: Int)
        fun onTextSelected(selection: TextSelectionData, rawPoint: PointF)

        fun hideTextSelectionOptionWindow()
        fun onTextSelectionCleared()
        fun onNotesStampsClicked(notes: List<CommentModel>, pointOfNote: PointF)
        fun loadTopPdfChunk(mergeId: Int, pageIndexToLoad: Int)
        fun loadBottomPdfChunk(mergedId: Int, pageIndexToLoad: Int)
        fun onScrolling()
        fun onTap()
        fun onMergeStart(mergeId: Int, mergeType: PdfFile.MergeType)
        fun onMergeEnd(mergeId: Int, mergeType: PdfFile.MergeType)
        fun onMergeFailed(mergeId: Int, mergeType: PdfFile.MergeType, message: String, exception: java.lang.Exception?)
    }
}
