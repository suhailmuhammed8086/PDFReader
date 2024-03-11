/**
 * Copyright 2017 Bartosz Schiller
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.pdfnotemate.tools.pdf.viewer.link

import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.pdfnotemate.tools.pdf.viewer.model.LinkTapEvent
import com.example.pdfnotemate.tools.pdf.viewer.PDFView

class DefaultLinkHandler(private val pdfView: PDFView) : LinkHandler {

    override fun handleLinkEvent(event: LinkTapEvent?) {
        val uri = event?.link?.uri
        val page = event?.link?.destPageIdx
        if (!uri.isNullOrEmpty()) {
            handleUri(uri)
        } else page?.let { handlePage(it) }
    }

    private fun handleUri(uri: String) {
        val parsedUri = Uri.parse(uri)
        val intent = Intent(Intent.ACTION_VIEW, parsedUri)
        val context = pdfView.context
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Log.w(TAG, "No activity found for URI: $uri")
        }
    }

    private fun handlePage(page: Int) {
        pdfView.jumpTo(page)
    }

    companion object {
        private val TAG = DefaultLinkHandler::class.java.simpleName
    }
}