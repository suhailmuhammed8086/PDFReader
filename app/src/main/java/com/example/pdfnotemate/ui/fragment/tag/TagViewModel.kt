package com.example.pdfnotemate.ui.fragment.tag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdfnotemate.repository.PDFRepository
import com.example.pdfnotemate.tools.OperationsStateHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TagViewModel @Inject constructor(
    private val pdfRepository: PDFRepository
) : ViewModel() {

    val tagListResponse = OperationsStateHandler(viewModelScope)
    val addTagResponse = OperationsStateHandler(viewModelScope)
    val removeTagResponse = OperationsStateHandler(viewModelScope)

    fun loadAllTags() {
        tagListResponse.load {
            pdfRepository.getAllTags()
        }
    }

    fun addTag(title: String) {
        addTagResponse.load {
            pdfRepository.addTag(title,"")
        }
    }

    fun removeTag(tagId: Long) {
        removeTagResponse.load {
            pdfRepository.removeTagById(tagId)
        }
    }
}