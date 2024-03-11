package com.example.pdfnotemate

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class NoteMateApp : Application() {


    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(this.applicationContext)

    }
}