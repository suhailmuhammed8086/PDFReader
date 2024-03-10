package com.example.pdfnotemate.ui.activity.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdfnotemate.repository.PDFRepository
import com.example.pdfnotemate.tools.OperationsStateHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val pdfRepository: PDFRepository
) : ViewModel(){


    val pdfListResponse = OperationsStateHandler(viewModelScope)

    fun getAllPdfs() {
        pdfListResponse.load {
            pdfRepository.getAllPdfs()
        }
    }
}