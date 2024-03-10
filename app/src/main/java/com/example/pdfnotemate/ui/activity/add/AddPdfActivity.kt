package com.example.pdfnotemate.ui.activity.add

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.pdfnotemate.R
import com.example.pdfnotemate.base.ui.BaseActivity
import com.example.pdfnotemate.databinding.ActivityAddPdfBinding
import com.example.pdfnotemate.model.PdfNoteListModel
import com.example.pdfnotemate.state.ResponseState
import com.example.pdfnotemate.tools.AppFileManager
import com.example.pdfnotemate.tools.FileDownloadTool
import com.example.pdfnotemate.utils.Alerts
import com.example.pdfnotemate.utils.BundleArguments
import com.example.pdfnotemate.utils.ProgressButtonHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class AddPdfActivity : BaseActivity(), View.OnClickListener {

    private val viewModel: AddPdfViewModel by viewModels()
    private lateinit var binding: ActivityAddPdfBinding
    private val downloadProgressButtonHelper = ProgressButtonHelper()

    enum class PageType {
        PickFromGallery,
        DownloadPdf
    }

    companion object {
        const val RESULT_ACTION_PDF_ADDED = "action.new.pdf.added"
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
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }

        getDataFromIntent()
        initView()

    }

    private fun initView() {
        downloadProgressButtonHelper.attachViews(
            binding.btDownload,
            binding.tvDownload,
            binding.downloadProgress
        )

        binding.btDownload.setOnClickListener(this)
        binding.btAddNewPdf.setOnClickListener(this)
        binding.btPickPdf.setOnClickListener(this)
        binding.btRemovePdf.setOnClickListener(this)

        // showing download or pick section
        showOrHideAddSection(true)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btDownload-> {
                downloadPdf()
            }
            R.id.btAddNewPdf-> {
                addPdf()
            }
            R.id.btPickPdf-> {
                pickPdfFromGallery()
            }
            R.id.btRemovePdf-> {
                removePdf()
            }
        }
    }

    private fun getDataFromIntent() {
        intent?.let {
            val pageType = it.getStringExtra(BundleArguments.ARGS_PAGE_TYPE)
            this.pageType = PageType.valueOf(pageType ?: PageType.PickFromGallery.name)
        }
    }

    private fun addPdf() {
        viewModel.addPdf(
            binding.etPdfTitle.text.toString().trim(),
            binding.etAbout.text.toString().trim(),
            null
        )
    }

    private fun removePdf() {
        viewModel.pdfFile?.delete()
        viewModel.pdfFile = null
        binding.pdfImportSuccessSection.visibility = View.GONE
        showOrHideAddSection(show = true)
    }

    private fun downloadPdf() {
        val url = binding.etPdfUrl.text.toString().trim()
        val saveFolder = AppFileManager.getPdfNoteFolder(this)
        if (url.isEmpty()) {
            Alerts.warningSnackBar(binding.root, "Enter url to download")
            return
        }
        if (saveFolder != null) {
            viewModel.downloadPdf(url, saveFolder, downloadPdfCallback)
        }
    }

    private var downloadPdfCallback = object : FileDownloadTool.DownloadCallback {
        override fun onDownloadStart() {
            downloadProgressButtonHelper.start()
        }

        override fun onDownloadFailed(error: String?) {
            downloadProgressButtonHelper.stop()
            Alerts.failureSnackBar(binding.root,error?:"Failed to download pdf")
        }

        override fun onDownloading(progress: Double) {
            binding.downloadProgress.progress = progress.toInt()
        }

        override fun onDownloadCompleted(file: File) {
            downloadProgressButtonHelper.stop()
            viewModel.pdfFile = file
            showOrHideAddSection(show = false)
            binding.pdfImportSuccessSection.visibility = View.VISIBLE
        }
    }


    private fun showOrHideAddSection(show: Boolean) {
        if (show) {
            if (pageType == PageType.DownloadPdf) {
                binding.downloadSection.visibility = View.VISIBLE
            } else {
                binding.pickFromGallerySection.visibility = View.VISIBLE
            }
        } else {
            if (pageType == PageType.DownloadPdf) {
                binding.downloadSection.visibility = View.GONE
            } else {
                binding.pickFromGallerySection.visibility = View.GONE
            }
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
            val uri = result.data?.data
            if (uri != null) {
                contentResolver?.openInputStream(uri)?.use {input->
                    val saveFile = AppFileManager.getNewPdfFile(this)
                    FileOutputStream(saveFile).use {
                        input.copyTo(it)
                    }
                    viewModel.pdfFile = saveFile
                    showOrHideAddSection(false)
                    binding.pdfImportSuccessSection.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun bindUI() = launch(Dispatchers.Main) {
        viewModel.pdfAddResponse.state.observe(this@AddPdfActivity) { state ->
            when (state) {
                ResponseState.Loading -> {

                }

                is ResponseState.Success<*> -> {
                    val response = state.response as PdfNoteListModel?
                    if (response != null) {
                        val result = Intent(RESULT_ACTION_PDF_ADDED)
                        setResult(RESULT_OK, result)
                        finish()
                    }
                }

                is ResponseState.ValidationError -> {
                    Alerts.warningSnackBar(binding.root, state.error)
                }

                is ResponseState.Failed -> {
                    Alerts.failureSnackBar(binding.root, state.error)
                }
            }
        }
    }
}