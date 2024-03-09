package com.example.pdfnotemate.ui.activity.home

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pdfnotemate.R
import com.example.pdfnotemate.base.ui.BaseActivity
import com.example.pdfnotemate.databinding.ActivityHomeBinding
import com.example.pdfnotemate.ui.activity.add.AddPdfActivity
import com.example.pdfnotemate.ui.fragment.MoreOptionModel
import com.example.pdfnotemate.ui.fragment.OptionPickFragment
import com.example.pdfnotemate.utils.BundleArguments

class HomeActivity : BaseActivity(), View.OnClickListener, OptionPickFragment.Listener {
    private lateinit var binding: ActivityHomeBinding

    companion object {
        private const val FROM_GALLERY = 1
        private const val DOWNLOAD_PDF = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initView()
    }

    private fun initView() {
        binding.btAddNewPdf.setOnClickListener(this)



    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btAddNewPdf -> {
                showAddOptions()
            }
        }
    }

    private fun showAddOptions() {
        val options = arrayListOf(
            MoreOptionModel(FROM_GALLERY, "Pick From Gallery"),
            MoreOptionModel(DOWNLOAD_PDF, "Download PDF")
        )

        OptionPickFragment.show(
            supportFragmentManager,
            "Add New PDF",
            options
        )
    }

    override fun onMoreOptionSelected(option: MoreOptionModel) {
        launchTo(AddPdfActivity::class.java){
            val pageType = if (option.id == FROM_GALLERY) {
                AddPdfActivity.PageType.PickFromGallery
            } else {
                AddPdfActivity.PageType.DownloadPdf
            }
            it.putString(BundleArguments.ARGS_PAGE_TYPE, pageType.name)
        }
    }



}