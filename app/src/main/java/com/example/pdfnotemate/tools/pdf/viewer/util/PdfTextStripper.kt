package com.example.pdfnotemate.tools.pdf.viewer.util

import android.graphics.PointF
import com.example.pdfnotemate.tools.pdf.viewer.model.PdfChar
import com.example.pdfnotemate.tools.pdf.viewer.model.PdfLine
import com.example.pdfnotemate.tools.pdf.viewer.model.PdfWord
import com.example.pdfnotemate.tools.pdf.viewer.model.Size
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition

@Suppress("UNCHECKED_CAST")
class PdfTextStripper(
    private var page : Int,
    private var lineIdStart : Int =0,
    private var wordIdStart : Int =0,
    private var charIdStart : Int =0,
) : PDFTextStripper() {

    private val textCoordinators = arrayListOf<PdfLine>()

    fun reset(){
        textCoordinators.clear()

    }


    fun updateInfo(
        page: Int = 0,
        lineIdStart: Int = 0,
        wordIdStart: Int = 0,
        charIdStart: Int = 0,
    ) {
        this.page = page
        this.lineIdStart = lineIdStart
        this.wordIdStart = wordIdStart
        this.charIdStart = charIdStart
    }


    override fun writeString(text: String?, textPositions: MutableList<TextPosition>?) {
//        "text $text => $textPositions".log("textPositions")
        super.writeString(text, textPositions)
        if (text?.trim().isNullOrEmpty()|| textPositions == null) return
        lineIdStart++
        try {
            val result = extractWords(text!!, textPositions)
            if (result != null){
                val words = result[WORDS] as ArrayList<PdfWord>
                val size = result[SIZE] as Size
                val position = result[POSITION] as PointF
                val pdfLine = PdfLine(lineIdStart,text, position, size)
                pdfLine.words.addAll(words)
                textCoordinators.add(pdfLine)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

//
//    private fun extractWords(text: String, textPosition: List<TextPosition>) : HashMap<String,Any>?{
//        val result : HashMap<String, Any> = hashMapOf()
//        val pdfWords = ArrayList<PdfWord>()
//        var wordStrings = text.split(" ")
//
//        if (wordStrings.isEmpty() && text.isNotEmpty()){
//            wordStrings = listOf(text)
//        }
//
//        Log.e(TAG, "extractWords() called with: text = ${text.getOrNull(0)}, words : $wordStrings ,  textPosition = ${textPosition.getOrNull(0)}")
//        var lastIndex = 0
//        wordStrings.forEach {
//            val startIndex = lastIndex
//            val endIndex = startIndex + it.length
//            lastIndex = endIndex+1
//            val textPosList = if (endIndex + 1 > textPosition.size) textPosition.subList(
//                startIndex,
//                textPosition.size
//            ) else textPosition.subList(startIndex, endIndex + 1)
//            val charResult =extractChars(textPosList)
//            if (charResult != null){
//                val chars = charResult[CHARS] as List<PdfChar>
//                val position = charResult[POSITION] as PointF
//                val size = charResult[SIZE] as Size
//                val word = PdfWord(
//                    wordIdStart,
//                    lineIdStart,
//                    it,
//                    position, size
//                )
//                word.characters.addAll(chars)
//                pdfWords.add(word)
//                wordIdStart++
//            }
//
//        }
//        if (pdfWords.isNotEmpty()){
//            val startWord = pdfWords[0]
//            val endWord = pdfWords[pdfWords.lastIndex]
//            val y = minOf(startWord.position.y, endWord.position.y)
//            val heightStartY = (startWord.position.y + startWord.size.height)
//            val heightEndY = (endWord.position.y + endWord.size.height)
//            val maxHeightY = maxOf(heightStartY,heightEndY)
//            val height = maxHeightY - y
//            val width = (endWord.position.x+endWord.size.width) - startWord.position.x
//
//            result[WORDS] = pdfWords
//            result[POSITION] = PointF(startWord.position.x,y)
//            result[SIZE] = Size(width, height)
//            return result
//        }
//        return  null
//    }

    private fun extractWords(text: String, textPosition: List<TextPosition>) : HashMap<String,Any>?{
        val result : HashMap<String, Any> = hashMapOf()
        val pdfWords = ArrayList<PdfWord>()
        val wordStrings = splitByWords(textPosition)
        wordStrings.forEach {
            val charResult = extractChars(it.textPositions)
            if (charResult != null){
                val chars = charResult[CHARS] as List<PdfChar>
                val position = charResult[POSITION] as PointF
                val size = charResult[SIZE] as Size
                val word = PdfWord(
                    wordIdStart,
                    lineIdStart,
                    it.word,
                    position, size
                )
                word.characters.addAll(chars)
                pdfWords.add(word)
                wordIdStart++
            }
        }
        if (pdfWords.isNotEmpty()){
            val startWord = pdfWords[0]
            val endWord = pdfWords[pdfWords.lastIndex]
            val y = minOf(startWord.position.y, endWord.position.y)
            val heightStartY = (startWord.position.y + startWord.size.height)
            val heightEndY = (endWord.position.y + endWord.size.height)
            val maxHeightY = maxOf(heightStartY,heightEndY)
            val height = maxHeightY - y
            val width = (endWord.position.x+endWord.size.width) - startWord.position.x

            result[WORDS] = pdfWords
            result[POSITION] = PointF(startWord.position.x,y)
            result[SIZE] = Size(width, height)
            return result
        }
        return  null
    }

    private fun splitByWords(textPosition: List<TextPosition>): List<WordExtractionResult> {
        val tempList = arrayListOf<TextPosition>()
        val wordExtractionResult = arrayListOf<WordExtractionResult>()
        var word = ""
        for (pos in textPosition){
            tempList.add(pos)
            if (pos.unicode[0].isRightToLeft()){
                word = pos.unicode + word
            }else {
                word += pos.unicode
            }
            if (pos.unicode.isBlank()){
                wordExtractionResult.add(WordExtractionResult(word,tempList.toList()))
                tempList.clear()
                word = ""
            }
        }
        if (tempList.isNotEmpty()){
            wordExtractionResult.add(WordExtractionResult(word,tempList.toList()))
        }
        return wordExtractionResult
    }

    data class WordExtractionResult(
        val word: String,
        val textPositions: List<TextPosition>
    ){
        override fun toString(): String {
            return word
        }
    }

    private fun extractChars(textPosition: List<TextPosition>) : HashMap<String, Any>? {
        val result : HashMap<String, Any> = hashMapOf()
        val pdfChars = ArrayList<PdfChar>()
        val wordPosition = PointF()

        textPosition.forEach {
//            val padding = it.height * 0.1890f * 3f // FIXME: Need to fix here
            val padding = it.height * 0.40f // FIXME: Need to fix here
            val topPosition = PointF(it.xDirAdj,it.yDirAdj - it.height-padding)
            val bottomPosition = PointF(it.xDirAdj,it.endY - padding)
            val size = Size(it.width,it.height+(padding*2))
            val char = PdfChar(
                charIdStart,
                lineIdStart,
                wordIdStart,
                it.unicode,
                topPosition,
                bottomPosition,
                size,
                page
            )
            pdfChars.add(char)
            charIdStart++
        }

        if (pdfChars.isNotEmpty()){
            val startChar = pdfChars[0]
            val endChar = pdfChars[pdfChars.lastIndex]
            val y = minOf(startChar.topPosition.y,endChar.topPosition.y)
            val heightStartY = (startChar.topPosition.y + startChar.size.height)
            val heightEndY = (endChar.topPosition.y + endChar.size.height)
            val maxHeightY = maxOf(heightStartY,heightEndY)
            val height = maxHeightY - y
            val width = (endChar.topPosition.x+endChar.size.width) - startChar.topPosition.x
            wordPosition.set(startChar.topPosition.x,y)
            result[CHARS] = pdfChars
            result[POSITION] = wordPosition
            result[SIZE] = Size(width, height)
            return result
        }
        return null
    }


    fun getLastLineId(): Int {
        return lineIdStart
    }
    fun getLastWordId(): Int {
        return wordIdStart
    }
    fun getLastCharId(): Int {
        return charIdStart
    }

    fun getTextCoordinates(): ArrayList<PdfLine> {
//        textCoordinators.sortBy { it.position.y }
        return textCoordinators
    }

    private fun Char.isRightToLeft(): Boolean {
        val charDir = Character.getDirectionality(this)
        return charDir == Character.DIRECTIONALITY_RIGHT_TO_LEFT
                || charDir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
                || charDir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING
                || charDir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE
    }

    companion object {
        private const val WORDS = "WORDS"
        private const val CHARS = "CHARS"
        private const val POSITION = "POSITION"
        private const val SIZE = "SIZE"
        private const val TAG = "PdfTextStripper"
    }

}


