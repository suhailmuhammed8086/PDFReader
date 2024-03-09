package com.example.pdfnotemate.ui.activity.add

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdfnotemate.data.ValidationErrorException
import com.example.pdfnotemate.repository.PDFRepository
import com.example.pdfnotemate.state.ResponseState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AddPdfViewModel @Inject constructor(
    private val pdfRepository: PDFRepository
) : ViewModel() {
    var pdfFile: File? = null

    private var pdfAddResponse = MutableLiveData<ResponseState>()


    private fun validation(title: String?, filePath: String?) {
        if (title.isNullOrEmpty()) {
            throw ValidationErrorException(1, "Please enter title of the book")
        }
        if (filePath.isNullOrEmpty()) {
            throw ValidationErrorException(2, "Please select or download pdf first.")
        }
    }

    fun addPdf(
        title: String,
        about: String,
        tagId: Long
    ) {
        try {
            validation(title, pdfFile?.absolutePath)
            pdfAddResponse.postValue(ResponseState.Loading)
            viewModelScope.launch(Dispatchers.IO) {
                val response = pdfRepository.addNewPdf(
                    pdfFile?.absolutePath ?: "",
                    title,
                    about,
                    tagId
                )
                pdfAddResponse.postValue(response)
            }
        } catch (e: ValidationErrorException) {
            pdfAddResponse.postValue(
                ResponseState.ValidationError(
                    e.errorCode,
                    e.message ?: "Something went wrong"
                )
            )
        } catch (e: Exception) {
            pdfAddResponse.postValue(ResponseState.Failed(e.message ?: "Something went wrong"))
        }


    }
}