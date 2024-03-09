package com.example.pdfnotemate.ui.activity.add

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pdfnotemate.R
import com.example.pdfnotemate.base.ui.BaseActivity
import com.example.pdfnotemate.databinding.ActivityAddPdfBinding
import com.example.pdfnotemate.tools.AppFileManager
import com.example.pdfnotemate.utils.BundleArguments
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class AddPdfActivity : BaseActivity(), View.OnClickListener {

    private val viewModel: AddPdfViewModel by viewModels()
    private lateinit var binding: ActivityAddPdfBinding

    enum class PageType {
        PickFromGallery,
        DownloadPdf
    }

    private var pageType: PageType = PageType.PickFromGallery

    private var pdfFilePickResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ::onPdfPickResult
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddPdfBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        getDataFromIntent()
        initView()
        pickPdfFromGallery()

    }

    private fun initView() {

    }

    override fun onClick(v: View?) {

    }

    private fun getDataFromIntent() {
        intent?.let {
            val pageType = it.getStringExtra(BundleArguments.ARGS_PAGE_TYPE)
            this.pageType = PageType.valueOf(pageType ?: PageType.PickFromGallery.name)
        }
    }


    private fun pickPdfFromGallery() {
        val pickIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        pdfFilePickResultLauncher.launch(pickIntent)
    }

    private fun onPdfPickResult(result: ActivityResult) {
        if (result.resultCode == RESULT_OK) {
            Log.e("TAG", "onPdfPickResult: ${result.data?.data}")
            val file = result.data?.data?.toFile()
            if (file != null) {
                val saveFile = AppFileManager.getNewPdfFile(this)
                file.copyTo(saveFile, overwrite = true)
                viewModel.pdfFile = saveFile
            }
        }
    }
}