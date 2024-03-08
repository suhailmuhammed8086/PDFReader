package com.example.pdfnotemate.base.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {


    /**Launch to new activity*/
    fun launchTo(activity: Class<*>, params: Bundle? = null, finishThisActivity: Boolean = false) {
        val launchIntent = Intent(this, activity)
        if (params != null) {
            launchIntent.putExtras(params)
        }
        startActivity(launchIntent)
        if (finishThisActivity) {
            finish()
        }
    }
}