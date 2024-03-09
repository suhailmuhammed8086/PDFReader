package com.example.pdfnotemate.repository

import com.example.pdfnotemate.model.PdfNoteListModel
import com.example.pdfnotemate.model.TagModel
import com.example.pdfnotemate.room.Dao
import com.example.pdfnotemate.room.entity.PdfNoteEntity
import com.example.pdfnotemate.state.ResponseState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject


class PdfRepositoryImpl @Inject constructor(
    private val dao: Dao,
) : PDFRepository {

    override suspend fun addNewPdf(
        filePath: String,
        title: String,
        about: String?,
        tagId: Long?
    ): ResponseState {
        return withContext(Dispatchers.IO) {
            try {
                val pdfEntry = PdfNoteEntity(
                    null,
                    title,
                    filePath,
                    about,
                    tagId,
                    System.currentTimeMillis()
                )

                val id = dao.addPdfNote(pdfEntry)
                if (id != -1L){
                    val tagModel = tagId?.let { dao.getTagById(it) }?.let {
                        TagModel(it.id ?: -1, it.title, it.colorCode)
                    }
                    val model = PdfNoteListModel(
                        id,
                        title,
                        tagModel,
                        about,
                        pdfEntry.updateAt
                    )
                    return@withContext ResponseState.Success<PdfNoteListModel>(model)
                }
                throw java.lang.Exception("Failed to add pdf")
            } catch (e: Exception) {
                return@withContext ResponseState.Failed(e.message ?: "Something went wrong")
            }
        }
    }
}