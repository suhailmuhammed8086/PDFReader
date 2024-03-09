package com.example.pdfnotemate.repository

import com.example.pdfnotemate.state.ResponseState

interface PDFRepository {

    suspend fun addNewPdf(
        filePath: String,
        title: String,
        about: String?,
        tagId: Long?
    ) : ResponseState
}