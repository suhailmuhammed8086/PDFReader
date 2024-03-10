package com.example.pdfnotemate.utils

import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar

class ProgressButtonHelper {

    private var containerView: ViewGroup? = null
    private var textView: View? = null
    private var progressView: ProgressBar? = null

    private var isLoading = false
    private var disableViewOnProgress = false


    fun attachViews(
        containerView: ViewGroup,
        textView: View?,
        progressView: ProgressBar
    ): ProgressButtonHelper {
        this.containerView = containerView
        this.textView = textView
        this.progressView = progressView
        return this
    }


    fun disableViewOnProgress(disable : Boolean){
        disableViewOnProgress = disable
    }




    fun start(){
        if (isLoading) return
        isLoading = true
        textView?.visibility = View.GONE
        progressView?.visibility = View.VISIBLE
        if (disableViewOnProgress){
            containerView?.isEnabled = false
//            containerView?.alpha = 0.4f
        }
    }

    fun stop(){
        isLoading = false
        textView?.visibility = View.VISIBLE
        progressView?.visibility = View.GONE
        if (disableViewOnProgress){
            containerView?.isEnabled = true
//            containerView?.alpha = 1f
        }
    }
}