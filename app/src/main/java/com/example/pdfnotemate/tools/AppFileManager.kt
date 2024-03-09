package com.example.pdfnotemate.tools

import android.content.Context
import android.content.ContextWrapper
import java.io.File
import java.util.UUID

object AppFileManager {
    fun getPdfNoteFolder(context: Context): File? {
        val cw = ContextWrapper(context)
        val dir = cw.getDir(PDF_FOLDER,Context.MODE_PRIVATE)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getRandomFileName(ext: String? = null): String {
        var name = UUID.randomUUID().toString().replace("-", "")
        if (!ext.isNullOrEmpty()) {
            name += ".$ext"
        }
        return name
    }

    fun getNewPdfFile(context: Context): File {
        return File(getPdfNoteFolder(context), getRandomFileName("pdf"))
    }


    private const val PDF_FOLDER = "PdfFolder"
}