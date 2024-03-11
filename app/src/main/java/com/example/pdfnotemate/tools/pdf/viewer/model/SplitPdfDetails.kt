package com.example.pdfnotemate.tools.pdf.viewer.model

class SplitPdfDetails(
    val startPageIndex: Int,
    val endPageIndex: Int,
    val pdfUrl: String,
) {
    var isDownloading = false
    val filePath: String? = null

    fun isContainPage(pageIndex: Int): Boolean {
        return pageIndex in startPageIndex..endPageIndex
    }
}
