package com.example.pdfnotemate.tools.pdf.viewer
import com.example.pdfnotemate.tools.pdf.viewer.model.PageModel
import com.example.pdfnotemate.tools.pdf.viewer.source.ByteArraySource
import com.example.pdfnotemate.tools.pdf.viewer.source.DocumentSource
import com.example.pdfnotemate.tools.pdf.viewer.source.FileSource
import com.example.pdfnotemate.tools.pdf.viewer.util.PdfTextStripper
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import com.shockwave.pdfium.util.Size
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.lang.ref.WeakReference

class PdfDecoder(
    pdfView: PDFView,
    private val pdfiumCore: PdfiumCore?,
    /**This is the pdf file, we need to pass it here to extract characters*/

) {

    private var cancelled = false
    private val pdfViewReference = WeakReference(pdfView)
    private lateinit var pdfFile: PdfFile

    private var lineId: Int = 0
    private var wordId: Int = 0
    private var charId: Int = 0

    suspend fun decode(
        docSource: DocumentSource,
        password: String?,
    ) {
        val result = withContext(Dispatchers.Default) {
            try {
                val pdfView = pdfViewReference.get()
                if (pdfView != null) {
                    val pdfDocument = docSource.createDocument(pdfView.context, pdfiumCore, password)
                    // extracting characters if file is provided, Note that this is important for text selection and annotating
//                    val pageDetails = getPdfPageDetails(docSource.getBytes()) ?: arrayListOf()
//                    val pageDetails = docSource.getFile()?.let { getPdfPageDetails(it) } ?: arrayListOf()
                    val pageDetails = when (docSource) {
                        is FileSource -> getPdfPageDetails(docSource.getFile())
                        is ByteArraySource -> getPdfPageDetails(docSource.getBytes())
                        else -> arrayListOf()
                    }
                    pdfFile = PdfFile(
                        pdfiumCore, pdfDocument, pdfView.pageFitPolicy,
                        getViewSize(pdfView), null, pdfView.isSwipeVertical,
                        pdfView.spacingPx, pdfView.isAutoSpacingEnabled,
                        pdfView.isFitEachPage,
                        pageDetails,
                        docSource.getStartPageIndex(),
                    )
                    null
                } else {
                    NullPointerException("pdfView == null")
                }
            } catch (t: Exception) {
                t.printStackTrace()
                t
            }
        }

        val pdfView = pdfViewReference.get()
        if (pdfView != null) {
            withContext(Dispatchers.Main) {
                if (result != null) {
                    pdfView.loadError(result)
                } else if (!cancelled) {
                    pdfView.loadComplete(pdfFile)
                }
            }
        }
    }

    suspend fun decodeToMergeFile(
        requestCode: Int,
        docSource: DocumentSource,
        password: String?,
        mergeId: Int,
    ) {
        var pdfDocument: PdfDocument? = null
        var pageDetails: ArrayList<PageModel>? = null
        val result = withContext(Dispatchers.Default) {
            try {
                val pdfView = pdfViewReference.get()
                if (pdfView != null) {
                    pageDetails = when (docSource) {
                        is FileSource -> getPdfPageDetails(docSource.getFile())
                        is ByteArraySource -> getPdfPageDetails(docSource.getBytes())
                        else -> arrayListOf()
                    }
                    pdfDocument = docSource.createDocument(pdfView.context, pdfiumCore, password)
                    // extracting characters if file is provided, Note that this is important for text selection and annotating
                    null
                } else {
                    NullPointerException("pdfView == null")
                }
            } catch (t: Exception) {
                t
            }
        }

        val pdfView = pdfViewReference.get()
        if (pdfView != null) {
            withContext(Dispatchers.Main) {
                if (result != null) {
                    pdfView.mergeLoadError(requestCode, mergeId, result)
                } else if (!cancelled) {
                    pdfView.onMergedPdfLoaded(
                        requestCode,
                        mergeId,
                        pdfDocument!!,
                        docSource.getStartPageIndex(),
                        pageDetails ?: arrayListOf(),
                        docSource.defaultPageIndexToLoad(),
                    )
                }
            }
        }
    }

    private fun getViewSize(pdfView: PDFView) = Size(pdfView.width, pdfView.height)

    fun cancel() {
        cancelled = true
    }

    /**This function will extract all the characters, words and lines in the pdf with their x,y,width and height*/
    private fun getPdfPageDetails(pdfFileBytes: ByteArray): ArrayList<PageModel> {
        val stream = ByteArrayInputStream(pdfFileBytes)
        val pdfDocument: PDDocument = PDDocument.load(stream, MemoryUsageSetting.setupTempFileOnly())
        val pageDetails = arrayListOf<PageModel>()
        for (index in 0 until pdfDocument.numberOfPages) {
            val documentPageRect = pdfDocument.getPage(index).mediaBox
            val textStripper = PdfTextStripper(index + 1, lineId, wordId, charId)
//            textStripper.sortByPosition = false
            textStripper.sortByPosition = true
            //  textStripper.increaseLevel()
            textStripper.startPage = index + 1
            textStripper.endPage = index + 1
            textStripper.getText(pdfDocument)

            lineId = textStripper.getLastLineId() + 1
            wordId = textStripper.getLastWordId() + 1
            charId = textStripper.getLastCharId() + 1
            pageDetails.add(PageModel(documentPageRect.width, documentPageRect.height, textStripper.getTextCoordinates()))
        }
        COSName.clearResources()
        pdfDocument.close()
        return pageDetails
    }
    private fun getPdfPageDetails(pdfFile: File): ArrayList<PageModel> {
        val pdfDocument: PDDocument = PDDocument.load(pdfFile)
//        val bytes = FileInputStream(pdfFile).use {
//            it.readBytes()
//        }
//        val pdfDocument : PDDocument =  PDDocument.load(bytes)
        val pageDetails = arrayListOf<PageModel>()
        for (index in 0 until pdfDocument.numberOfPages) {
            val documentPageRect = pdfDocument.getPage(index).mediaBox
            val textStripper = PdfTextStripper(index + 1, lineId, wordId, charId)
//            textStripper.sortByPosition = false
            textStripper.sortByPosition = true
            textStripper.startPage = index + 1
            textStripper.endPage = index + 1
            textStripper.getText(pdfDocument)

            lineId = textStripper.getLastLineId() + 1
            wordId = textStripper.getLastWordId() + 1
            charId = textStripper.getLastCharId() + 1
            pageDetails.add(PageModel(documentPageRect.width, documentPageRect.height, textStripper.getTextCoordinates()))
        }
        return pageDetails
    }
}
