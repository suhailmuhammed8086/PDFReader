/*
 * Copyright (C) 2016 Bartosz Schiller.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.pdfnotemate.tools.pdf.viewer.source

import android.content.Context
import android.os.ParcelFileDescriptor
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class FileSource(private val file: File, private val startPageIndex: Int = 0, private val defaultPageIndexToLoad: Int = 0) : DocumentSource {

    private var bytes: ByteArray = FileInputStream(file).use {
        it.readBytes()
    }

    @Throws(IOException::class)
    override fun createDocument(
        context: Context?,
        core: PdfiumCore?,
        password: String?,
    ): PdfDocument? {
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return core!!.newDocument(pfd, password)
    }

    override fun getBytes(): ByteArray {
        return bytes
    }

    override fun getFile(): File {
        return file
    }

    override fun getStartPageIndex(): Int {
        return startPageIndex
    }

    override fun defaultPageIndexToLoad(): Int {
        return defaultPageIndexToLoad
    }
}
