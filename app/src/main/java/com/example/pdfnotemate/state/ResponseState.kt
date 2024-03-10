package com.example.pdfnotemate.state

sealed class ResponseState {
    data object Loading: ResponseState()
    class Success<T>(val response: T?) : ResponseState()
    class ValidationError(val errorCode: Int,val error: String) : ResponseState()
    class Failed(val error: String) : ResponseState()
}