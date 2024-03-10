package com.example.pdfnotemate.ui.activity.entry

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pdfnotemate.R
import com.example.pdfnotemate.base.ui.BaseActivity
import com.example.pdfnotemate.databinding.ActivityEntryBinding
import com.example.pdfnotemate.ui.activity.home.HomeActivity

class EntryActivity : BaseActivity(), View.OnClickListener {
    private lateinit var binding: ActivityEntryBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initView()
    }

    private fun initView() {
        binding.btStart.setOnClickListener(this)
        binding.btStart.callOnClick()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btStart -> {
                launchTo(HomeActivity::class.java, finishThisActivity = true)
            }
        }
    }
}