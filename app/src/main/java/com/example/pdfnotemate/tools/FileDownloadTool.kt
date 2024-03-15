package com.example.pdfnotemate.tools

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URLEncoder

object FileDownloadTool {

    suspend fun downloadFile(url: String, saveFileFolder: File, callback: DownloadCallback? = null) {
        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(url)
                    .build()
                withContext(Dispatchers.Main) {
                    callback?.onDownloadStart()
                }
                val response = client.newCall(request).execute()

                val tempFile = File.createTempFile("PdfDownload","pdf")

                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        callback?.onDownloadFailed(response.message())
                    }
                    return@withContext
                }


                val responseBody = response.body()?: throw Exception("Failed to get data")
                val contentLength = responseBody.contentLength().toDouble()
                val inputStream = responseBody.byteStream()
                val outputStream = FileOutputStream(tempFile)
                val buffer =ByteArray(4096)
                var bytesRead : Int
                var totalBytesRead = 0
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead+= bytesRead

                    // updating progress.
                    val progress = (totalBytesRead.toDouble()/ contentLength) * 100
                    withContext(Dispatchers.Main) {
                        callback?.onDownloading(progress)
                    }
                }

                // creating permanent file

                val name = AppFileManager.getRandomFileName(".pdf")
                val saveFile = File(saveFileFolder, name)
                // Moving data to permanent file
                if (tempFile.exists()) {
                    FileInputStream(tempFile).use { inStream->
                        FileOutputStream(saveFile).use {
                            inStream.copyTo(it)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        callback?.onDownloadCompleted(saveFile)
                    }
                    tempFile.delete()
                    return@withContext
                }
                tempFile.delete()
                withContext(Dispatchers.Main) {
                    callback?.onDownloadFailed("Failed to save PDF")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback?.onDownloadFailed(e.message)
                }
                return@withContext
            }
        }
    }

    interface DownloadCallback {
        fun onDownloadStart()
        fun onDownloadFailed(error: String?)
        fun onDownloading(progress: Double)
        fun onDownloadCompleted(file: File)
    }
}
