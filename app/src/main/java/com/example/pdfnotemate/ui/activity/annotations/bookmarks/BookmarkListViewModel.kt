package com.example.pdfnotemate.ui.activity.annotations.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdfnotemate.repository.PDFRepository
import com.example.pdfnotemate.tools.OperationsStateHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BookmarkListViewModel @Inject constructor(
    private val pdfRepository: PDFRepository
) : ViewModel() {
    val deleteBookmarksResponse = OperationsStateHandler(viewModelScope)

    fun deleteBookmarks(ids: List<Long>) {
        deleteBookmarksResponse.load {
            pdfRepository.deleteBookmarks(ids)
        }
    }
}