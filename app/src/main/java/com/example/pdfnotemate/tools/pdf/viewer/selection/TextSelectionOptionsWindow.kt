package com.example.pdfnotemate.tools.pdf.viewer.selection

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import com.example.pdfnotemate.R

import com.example.pdfnotemate.tools.pdf.viewer.PDFView
import com.example.pdfnotemate.tools.pdf.viewer.model.Coordinates
import com.example.pdfnotemate.tools.pdf.viewer.model.TextSelectionData
import com.example.pdfnotemate.utils.Utils
import kotlin.math.roundToInt

class TextSelectionOptionsWindow(
    private val context: Context,
    private val listener: Listener,
) : View.OnClickListener {

    companion object {
        private const val CLOSE_ICON_ID = "CLOSE_ICON_STRING"
    }

    private lateinit var pdfView: PDFView
    private var popupWindow: PopupWindow? = null

    private var view: View? = null
    private var optionContainer: LinearLayoutCompat? = null
    private var colorContainer: LinearLayoutCompat? = null

    private val colorsList = listOf("#00DCD6", "#2B65F9", "#8C6BF8", "#FF5833", "#FFCC25", CLOSE_ICON_ID)
    private var colorOptionSize = 50
    private var selectedTextData: TextSelectionData? = null

    // we will use this value to arrange the popup
    private var optionWindowHeight = Utils.convertDpToPixel(context, 40f)

    fun attachToPdfView(pdfView: PDFView) {
        this.pdfView = pdfView
    }

    init {
        colorOptionSize = Utils.convertDpToPixel(context, 17f)
    }

    interface Listener {
        fun onAddHighlightClick(
            snippet: String,
            color: String,
            page: Int,
            coordinates: Coordinates,
        )

        fun onAddNotClick(snippet: String, page: Int, coordinates: Coordinates)
    }

//    fun show(x : Float,y:Float, selectedText : String){
//        this.selectedText = selectedText
//        if (popupWindow?.isShowing == true) return
//        val inflater = LayoutInflater.from(context)
//        view = inflater.inflate(R.layout.layout_selected_text_option_window,null)
//        optionContainer = view?.findViewById(R.id.optionContainer)
//        colorContainer = view?.findViewById(R.id.colorsSection)
//        val tvAddNote = view?.findViewById<AppCompatTextView>(R.id.tvAddNote)
//        val tvHighlight = view?.findViewById<AppCompatTextView>(R.id.tvHighLight)
//        tvAddNote?.setOnClickListener(this)
//        tvHighlight?.setOnClickListener(this)
//        popupWindow = PopupWindow(view, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
// //        popupWindow?.showAsDropDown(pdfView,x.roundToInt(),y.roundToInt())
//        popupWindow?.showAtLocation(pdfView,Gravity.BOTTOM,0,200)
//    }

    fun show(x: Float, y: Float, selectedText: TextSelectionData) {
        this.selectedTextData = selectedText
        if (popupWindow?.isShowing == true) return
        val inflater = LayoutInflater.from(context)
        view = inflater.inflate(R.layout.layout_selected_text_option_window, null)
        optionContainer = view?.findViewById(R.id.optionContainer)
        colorContainer = view?.findViewById(R.id.colorsSection)
        val tvAddNote = view?.findViewById<AppCompatTextView>(R.id.tvAddNote)
        val tvHighlight = view?.findViewById<AppCompatTextView>(R.id.tvHighLight)
        tvAddNote?.setOnClickListener(this)
        tvHighlight?.setOnClickListener(this)
        popupWindow = PopupWindow(
            view,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        var calculatedY = y - (optionWindowHeight * 1.2) // showing popup just above the textSelection
        if (calculatedY <= 0) {
            calculatedY = optionWindowHeight.toFloat() * 1.2
        }
        popupWindow?.showAsDropDown(pdfView, x.roundToInt(), calculatedY.roundToInt())
//        popupWindow?.showAtLocation(pdfView, Gravity.BOTTOM, 0, 200)
    }

    fun dismiss(clearTextSelection: Boolean = false) {
        popupWindow?.dismiss()
        if (clearTextSelection) {
            pdfView.clearAllTextSelectionAndCoordinates()
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tvAddNote -> {
                if (selectedTextData != null) {
                    val selectedText = selectedTextData!!.getSelectedText()
                    if (selectedText.isNotEmpty()) {
                        listener.onAddNotClick(
                            selectedText,
                            selectedTextData!!.getPdfPageNumber(),
                            selectedTextData?.getStartEndCoordinates()!!,
                        )
                    }
                }
//                dismiss(true)
            }
            R.id.tvHighLight -> {
                setColorPage()
            }
        }
    }

    private fun setColorPage() {
        optionContainer?.visibility = View.GONE
        colorContainer?.visibility = View.VISIBLE
        colorContainer?.removeAllViews()
        colorsList.forEach {
            val isCloseIcon = it == CLOSE_ICON_ID
            val view = if (isCloseIcon) {
                AppCompatImageView(context).apply {
                    layoutParams = LinearLayoutCompat.LayoutParams(colorOptionSize + 1, colorOptionSize + 1).apply {
                        marginStart = 7
                        marginEnd = 7
                        bottomMargin = 1
                        topMargin = 1
                    }
                    setImageResource(R.drawable.ic_close_white)
                    imageTintList = ColorStateList.valueOf(Color.BLACK)
                    scaleType = ImageView.ScaleType.FIT_XY
                    tag = it
                    setOnClickListener(onColorSelected)
                }
            } else {
                View(context).apply {
                    layoutParams = LinearLayoutCompat.LayoutParams(colorOptionSize, colorOptionSize).apply {
                        marginStart = 7
                        marginEnd = 7
                        bottomMargin = 1
                        topMargin = 1
                    }
                    background = ContextCompat.getDrawable(context, R.drawable.background_full_round_white)
                    backgroundTintList = ColorStateList.valueOf(Color.parseColor(it))
                    tag = it
                    setOnClickListener(onColorSelected)
                }
            }

            colorContainer?.addView(view)
        }
    }

    private val onColorSelected = View.OnClickListener {
        val tag = it.tag as String
        if (tag != CLOSE_ICON_ID) {
            var color = tag
//            if (color.length<=7) {color = color.replace("#","#99")}
            if (selectedTextData != null) {
                val selectedText = selectedTextData?.getSelectedText()
                if (!selectedText.isNullOrEmpty()) {
                    listener.onAddHighlightClick(
                        selectedText,
                        color,
                        selectedTextData!!.getPdfPageNumber(),
                        selectedTextData!!.getStartEndCoordinates(),
                    )
                }
            }
            dismiss(true)
        } else {
            colorContainer?.visibility = View.GONE
            optionContainer?.visibility = View.VISIBLE
        }
    }
}
