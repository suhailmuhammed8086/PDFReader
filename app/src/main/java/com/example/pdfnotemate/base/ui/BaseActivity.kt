package com.example.pdfnotemate.base.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {


    /**Launch to new activity*/
    fun launchTo(activity: Class<*>, finishThisActivity: Boolean = false) {
        val launchIntent = Intent(this, activity)
        startActivity(launchIntent)
        if (finishThisActivity) {
            finish()
        }
    }
    /**Launch to new activity*/
    fun launchTo(activity: Class<*>, finishThisActivity: Boolean = false, bundleScope: ((bundle: Bundle)->Unit)? = null) {
        val launchIntent = Intent(this, activity)

        if (bundleScope != null) {
            val bundle = Bundle()
            bundleScope.invoke(bundle)
            launchIntent.putExtras(bundle)
        }
        startActivity(launchIntent)
        if (finishThisActivity) {
            finish()
        }
    }
}