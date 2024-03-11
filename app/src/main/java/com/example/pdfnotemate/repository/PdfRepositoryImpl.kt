package com.example.pdfnotemate.repository

import com.example.pdfnotemate.model.AnnotationListResponse
import com.example.pdfnotemate.model.DeleteAnnotationResponse
import com.example.pdfnotemate.model.PdfNoteListModel
import com.example.pdfnotemate.model.PdfNotesResponse
import com.example.pdfnotemate.model.TagModel
import com.example.pdfnotemate.room.Dao
import com.example.pdfnotemate.room.entity.BookmarkEntity
import com.example.pdfnotemate.room.entity.CommentEntity
import com.example.pdfnotemate.room.entity.HighlightEntity
import com.example.pdfnotemate.room.entity.PdfNoteEntity
import com.example.pdfnotemate.room.entity.PdfTagEntity
import com.example.pdfnotemate.state.ResponseState
import com.example.pdfnotemate.tools.pdf.viewer.model.BookmarkModel
import com.example.pdfnotemate.tools.pdf.viewer.model.CommentModel
import com.example.pdfnotemate.tools.pdf.viewer.model.Coordinates
import com.example.pdfnotemate.tools.pdf.viewer.model.HighlightModel
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
                        filePath,
                        pdfEntry.updateAt,0,0,0
                    )
                    return@withContext ResponseState.Success<PdfNoteListModel>(model)
                }
                throw java.lang.Exception("Failed to add pdf")
            } catch (e: Exception) {
                return@withContext ResponseState.Failed(e.message ?: "Something went wrong")
            }
        }
    }

    override suspend fun getAllPdfs(): ResponseState {
        return withContext(Dispatchers.IO) {
            try {
               val notes = dao.getAllPdfNotes()
                val pdfNotes = notes.map {
                    val tagModel = it.tagId?.let {tagId-> dao.getTagById(tagId) }?.let { tag->
                        TagModel(tag.id ?: -1, tag.title, tag.colorCode)
                    }
                    PdfNoteListModel(
                        it.id?:-1,
                        it.title,
                        tagModel,
                        it.about,
                        it.filePath,
                        it.updateAt,0,0,0
                    )
                }
                return@withContext ResponseState.Success<PdfNotesResponse>(PdfNotesResponse(pdfNotes))

            } catch (e: Exception) {
                return@withContext ResponseState.Failed(e.message ?: "Something went wrong")
            }
        }
    }

    override suspend fun addTag(title: String, color: String): ResponseState {
        return withContext(Dispatchers.IO) {
            try {
                val tagEntity = PdfTagEntity(null,title,color)
                val id = dao.addPdfTag(tagEntity)
                return@withContext ResponseState.Success<TagModel>(TagModel(id,title,color))
            } catch (e: Exception) {
                return@withContext ResponseState.Failed(e.message ?: "Something went wrong")
            }
        }
    }

    override suspend fun addComment(
        pdfId: Long,
        snippet: String,
        text: String,
        page: Int,
        coordinates: Coordinates
    ): ResponseState {
        return withContext(Dispatchers.IO) {
            try {
                val commentEntity = CommentEntity(
                    null,
                    pdfId,
                    snippet,
                    text,
                    page,
                    System.currentTimeMillis(),
                    coordinates
                )
                val id = dao.insertComment(commentEntity)
                return@withContext ResponseState.Success<CommentModel>(
                    CommentModel(
                    id,
                    snippet,
                    text,
                    page,
                    commentEntity.updatedAt,
                    coordinates
                )
                )
            } catch (e: Exception) {
                return@withContext ResponseState.Failed(e.message ?: "Something went wrong")
            }
        }
    }

    override suspend fun getAllComments(pdfId: Long): ResponseState {
        return withContext(Dispatchers.IO) {
            try {
                val comments = dao.getCommentsOfPdf(pdfId).map {
                    CommentModel(
                        it.id?:-1L,
                        it.snippet,
                        it.text,
                        it.page,
                        it.updatedAt,
                        it.coordinates
                    )
                }
                return@withContext ResponseState.Success<List<CommentModel>>(comments)
            } catch (e: Exception) {
                return@withContext ResponseState.Failed(e.message ?: "Something went wrong")
            }
        }
    }

    override suspend fun deleteComments(commentIds: List<Long>): ResponseState {
        return withContext(Dispatchers.IO) {
            try {
                dao.deleteCommentsWithIds(commentIds)
                return@withContext ResponseState.Success<DeleteAnnotationResponse>(DeleteAnnotationResponse(commentIds))
            } catch (e: Exception) {
                return@withContext ResponseState.Failed(e.message ?: "Something went wrong")
            }
        }
    }

    override suspend fun addHighlight(
        pdfId: Long,
        snippet: String,
        color: String,
        page: Int,
        coordinates: Coordinates
    ): ResponseState {
        return withContext(Dispatchers.IO) {
            try {
                val highlightEntity = HighlightEntity(
                    null,
                    pdfId,
                    snippet,
                    color,
                    page,
                    System.currentTimeMillis(),
                    coordinates
                )
                val id = dao.insertHighlight(highlightEntity)
                return@withContext ResponseState.Success<HighlightModel>(
                    HighlightModel(
                        id,
                        snippet,
                        color,
                        page,
                        highlightEntity.updatedAt,
                        coordinates
                    )
                )
            } catch (e: Exception) {
                return@withContext ResponseState.Failed(e.message ?: "Something went wrong")
            }
        }
    }

    override suspend fun getAllHighlight(pdfId: Long): ResponseState {
        return withContext(Dispatchers.IO) {
            try {
                val highlights = dao.getHighlightsOfPdf(pdfId).map {
                    HighlightModel(
                        it.id?:-1L,
                        it.snippet,
                        it.color,
                        it.page,
                        it.updatedAt,
                        it.coordinates
                    )
                }
                return@withContext ResponseState.Success<List<HighlightModel>>(highlights)
            } catch (e: Exception) {
                return@withContext ResponseState.Failed(e.message ?: "Something went wrong")
            }
        }
    }

    override suspend fun deleteHighlight(highlightIds: List<Long>): ResponseState {
        return withContext(Dispatchers.IO) {
            try {
                dao.deleteCommentsWithIds(highlightIds)
                return@withContext ResponseState.Success<DeleteAnnotationResponse>(DeleteAnnotationResponse(highlightIds))
            } catch (e: Exception) {
                return@withContext ResponseState.Failed(e.message ?: "Something went wrong")
            }
        }
    }

    override suspend fun addBookmark(
        pdfId: Long,
        page: Int
    ): ResponseState {
        return withContext(Dispatchers.IO) {
            try {
                val bookmarkEntity = BookmarkEntity(
                    null,
                    pdfId,
                    page,
                    System.currentTimeMillis()
                )
                val id = dao.insertBookmark(bookmarkEntity)
                return@withContext ResponseState.Success<BookmarkModel>(
                    BookmarkModel(
                        id,
                        page,
                        bookmarkEntity.updatedAt,
                    )
                )
            } catch (e: Exception) {
                return@withContext ResponseState.Failed(e.message ?: "Something went wrong")
            }
        }
    }

    override suspend fun getAllBookmark(pdfId: Long): ResponseState {
        return withContext(Dispatchers.IO) {
            try {
                val bookmarks = dao.getBookmarksOfPdf(pdfId).map {
                    BookmarkModel(
                        it.id?:-1L,
                        it.page,
                        it.updatedAt
                    )
                }
                return@withContext ResponseState.Success<List<BookmarkModel>>(bookmarks)
            } catch (e: Exception) {
                return@withContext ResponseState.Failed(e.message ?: "Something went wrong")
            }
        }
    }

    override suspend fun deleteBookmark(bookmarkIds: List<Long>): ResponseState {
        return withContext(Dispatchers.IO) {
            try {
                dao.deleteBookmarksWithIds(bookmarkIds)
                return@withContext ResponseState.Success<Nothing>(null)
            } catch (e: Exception) {
                return@withContext ResponseState.Failed(e.message ?: "Something went wrong")
            }
        }
    }

    override suspend fun deleteBookmarkWithPageAndPdfId(page: Int, pdfId: Long): ResponseState {
        return withContext(Dispatchers.IO) {
            try {
                val bookmarksIds = dao.getBookmarksWithPageAndPdfId(page, pdfId).map { it.id?:-1 }
                dao.deleteBookmarksWithIds(bookmarksIds)
                return@withContext ResponseState.Success<DeleteAnnotationResponse>(DeleteAnnotationResponse(bookmarksIds))
            } catch (e: Exception) {
                return@withContext ResponseState.Failed(e.message ?: "Something went wrong")
            }
        }
    }

    override suspend fun getAllAnnotations(pdfId: Long): ResponseState {
        return withContext(Dispatchers.IO) {
            try {

                val comments = dao.getCommentsOfPdf(pdfId).map {
                    CommentModel(
                        it.id ?: -1L,
                        it.snippet,
                        it.text,
                        it.page,
                        it.updatedAt,
                        it.coordinates
                    )
                }

                val highlights = dao.getHighlightsOfPdf(pdfId).map {
                    HighlightModel(
                        it.id ?: -1L,
                        it.snippet,
                        it.color,
                        it.page,
                        it.updatedAt,
                        it.coordinates
                    )
                }

                val bookmarks = dao.getBookmarksOfPdf(pdfId).map {
                    BookmarkModel(
                        it.id ?: -1L,
                        it.page,
                        it.updatedAt
                    )
                }

                return@withContext ResponseState.Success<AnnotationListResponse>(
                    AnnotationListResponse(
                        ArrayList(comments),
                        ArrayList(highlights),
                        ArrayList(bookmarks),
                    )
                )
            } catch (e: Exception) {
                return@withContext ResponseState.Failed(e.message ?: "Something went wrong")
            }
        }
    }
}