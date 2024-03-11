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
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import java.io.File
import java.io.IOException

interface DocumentSource {
    @Throws(IOException::class)
    fun createDocument(context: Context?, core: PdfiumCore?, password: String?): PdfDocument?

    fun getBytes(): ByteArray

    /**get file of pdf, note that file will only get for FileSource*/
    fun getFile(): File?

    fun getStartPageIndex(): Int

    fun defaultPageIndexToLoad(): Int
}
