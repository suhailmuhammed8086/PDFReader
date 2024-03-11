package com.example.pdfnotemate.tools.pdf.viewer.model

import android.graphics.RectF

class TextSelectionData {
    private val lines = arrayListOf<CharDrawSegments>()
    var currentSelectionPageIndex = 0
    var currentSelectionPaginationIndex = -1

    fun clearAllSelection(clearSelectedPageDetails : Boolean = false){
        lines.clear()
        if (clearSelectedPageDetails){
            currentSelectionPageIndex = -1
            currentSelectionPaginationIndex = -1
        }
    }


    fun getSelections(): List<CharDrawSegments> {
        return lines.toList()
    }


    fun getSelectedText(): String {
        val result = java.lang.StringBuilder("")
        for (line in lines){
            for (char in line.chars){
                result.append(char.text)
            }
            result.append(" ")
        }
        return result.toString()
    }

    fun getPdfPageNumber(): Int {
       return if (currentSelectionPaginationIndex<0){
            return -1
        }else{
            currentSelectionPaginationIndex + 1
        }
    }

    fun getStartEndCoordinates() : Coordinates {
        if (lines.isNotEmpty()){
            val firstLine = lines.first()
            val lastLine = lines.last()
            return Coordinates(
                firstLine.startChar.bottomPosition.x.toDouble(),
                firstLine.startChar.bottomPosition.y.toDouble() + firstLine.startChar.size.height / 2,
                lastLine.endChar.bottomPosition.x.toDouble() + lastLine.endChar.size.width,
                lastLine.endChar.bottomPosition.y.toDouble() + lastLine.endChar.size.height / 2
            )
        }
        return Coordinates(0.0,0.0,0.0,0.0)
    }

    fun getChars(): ArrayList<PdfChar> {
        val chars = arrayListOf<PdfChar>()
        for (line in lines){
            for (char in line.chars){
                chars.add(char)
            }
        }
        return chars
    }

    fun hasTextSelected():Boolean{
        return lines.isNotEmpty()
    }

    fun addSelection(chars: ArrayList<PdfChar>) {
        if (chars.isNotEmpty()) { lines.add(CharDrawSegments(chars)) }
    }

    fun addWordsSelection(words: ArrayList<PdfWord>) {
        words.forEach { addSelection(it.characters) }
    }
    fun addLineSelection(lineSelectionDetails : ArrayList<CharDrawSegments>) {
        lines.addAll(lineSelectionDetails)
    }
}

data class CharDrawSegments(
    val chars : ArrayList<PdfChar> = arrayListOf()
){

    lateinit var startChar : PdfChar
    lateinit var endChar : PdfChar
    var rect : RectF


    init {
        rect = getCalculatedRect()
    }

    private fun getCalculatedRect() : RectF{
        if (chars.isNotEmpty()) {
            startChar = chars[0]
            endChar = chars[chars.lastIndex]
            val y = minOf(startChar.relatedPosition.y, endChar.relatedPosition.y)
            val heightStartY = (startChar.relatedPosition.y + startChar.relatedSize.height)
            val heightEndY = (endChar.relatedPosition.y + endChar.relatedSize.height)
            val maxHeightY = maxOf(heightStartY,heightEndY)
            val height = maxHeightY - y
            val width = (endChar.relatedPosition.x+endChar.relatedSize.width) - startChar.relatedPosition.x

            return RectF(
                startChar.relatedPosition.x,
                y ,
                (startChar.relatedPosition.x+width) ,
                (y + height)
            )
        }
        return RectF(0f,0f,0f,0f)
    }

    fun invalidate(){
        rect = getCalculatedRect()
    }
}

fun ArrayList<CharDrawSegments>.getAllChars(): List<PdfChar> {
    val chars = ArrayList<PdfChar>()
    forEach {
        chars.addAll(it.chars)
    }
    return chars.toList()
}