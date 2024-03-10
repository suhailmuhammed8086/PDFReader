package com.example.pdfnotemate.base.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

open class BaseActivity : AppCompatActivity(), CoroutineScope {

    private var job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindUI()
    }


    /**Launch to new activity*/
    fun launchTo(activity: Class<*>, finishThisActivity: Boolean = false) {
        val launchIntent = Intent(this, activity)
        startActivity(launchIntent)
        if (finishThisActivity) {
            finish()
        }
    }

    /**Launch to new activity*/
    fun launchTo(
        activity: Class<*>,
        finishThisActivity: Boolean = false,
        bundleScope: ((bundle: Bundle) -> Unit)? = null
    ) {
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

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()

    }

    open fun bindUI(): Job = launch { }
}