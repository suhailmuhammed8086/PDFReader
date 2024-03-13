package com.example.pdfnotemate.ui.activity.home

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pdfnotemate.R
import com.example.pdfnotemate.adapter.PdfListAdapter
import com.example.pdfnotemate.base.ui.BaseActivity
import com.example.pdfnotemate.databinding.ActivityHomeBinding
import com.example.pdfnotemate.model.PdfNoteListModel
import com.example.pdfnotemate.model.PdfNotesResponse
import com.example.pdfnotemate.state.ResponseState
import com.example.pdfnotemate.ui.activity.add.AddPdfActivity
import com.example.pdfnotemate.ui.activity.reader.PdfReaderActivity
import com.example.pdfnotemate.ui.fragment.MoreOptionModel
import com.example.pdfnotemate.ui.fragment.OptionPickFragment
import com.example.pdfnotemate.utils.Alerts
import com.example.pdfnotemate.utils.BundleArguments
import com.example.pdfnotemate.utils.log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeActivity : BaseActivity(), View.OnClickListener, OptionPickFragment.Listener,
    PdfListAdapter.Listener {
    private lateinit var binding: ActivityHomeBinding
    private val viewModel: HomeViewModel by viewModels()


    private var adapter: PdfListAdapter? = null
    private var pdfList = arrayListOf<PdfNoteListModel>()
    private var pdfListAll = arrayListOf<PdfNoteListModel>()

    companion object {
        private const val FROM_GALLERY = 1
        private const val DOWNLOAD_PDF = 2
    }

    private var addPdfLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                if (it.data?.action == AddPdfActivity.RESULT_ACTION_PDF_ADDED) {
                    Alerts.successSnackBar(binding.root, "Note added successfully.")
                    viewModel.getAllPdfs()
                }
            }
        }

    private var pdfReaderLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(),::onPdfReaderActivityResult)



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
        viewModel.getAllPdfs()
    }

    private fun initView() {
        binding.btAddNewPdf.setOnClickListener(this)
        binding.btBack.setOnClickListener(this)
        adapter = PdfListAdapter(pdfList, this)
        binding.rvPdfList.apply {
            adapter = this@HomeActivity.adapter
            layoutManager = LinearLayoutManager(this@HomeActivity)
        }

        binding.etSearch.doAfterTextChanged {
            filterPdfList(it?.toString())
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btBack -> {
                finish()
            }
            R.id.btAddNewPdf -> {
                showAddOptions()
            }
        }
    }

    override fun onPdfItemClicked(pdf: PdfNoteListModel) {
        val launchIntent = Intent(this, PdfReaderActivity::class.java)
        launchIntent.putExtra(BundleArguments.ARGS_PDF_DETAILS,pdf)
        pdfReaderLauncher.launch(launchIntent)
    }

    override fun onPdfItemLongClicked(pdf: PdfNoteListModel) {

    }

    @SuppressLint("NotifyDataSetChanged")
    override fun bindUI() = launch(Dispatchers.Main) {
        viewModel.pdfListResponse.state.observe(this@HomeActivity) { state ->
            when (state) {
                is ResponseState.Failed -> {
                    binding.progressBar.visibility = View.GONE
                    setError(state.error)
                }

                ResponseState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }

                is ResponseState.Success<*> -> {
                    binding.progressBar.visibility = View.GONE
                    val pdfListResponse = state.response as PdfNotesResponse?

                    if (pdfListResponse != null) {
                        pdfListAll.clear()
                        pdfListAll.addAll(pdfListResponse.notes)
                        pdfList.clear()
                        pdfList.addAll(pdfListAll)
                    }
                    pdfList.size.log("pdf size")
                    adapter?.notifyDataSetChanged()

                    if (pdfList.isEmpty()) {
                        setError(getString(R.string.you_don_t_have_any_pdf_notes_yet))
                    } else {
                        setError(null)
                    }
                }

                is ResponseState.ValidationError -> {

                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun filterPdfList(key: String?) {
        pdfList.clear()
        if (key.isNullOrEmpty()) {
            pdfList.addAll(pdfListAll)
        } else {
            pdfList.addAll(pdfListAll.filter {
                it.title.uppercase().contains(key.uppercase()) ||
                        it.tag?.title?.uppercase()?.contains(key.uppercase()) ?: false
            })
        }
        adapter?.notifyDataSetChanged()

        if (pdfList.isEmpty()) {
            setError("You don't have note with that details")
        } else {
            setError(null)
        }
    }


    private fun setError(error: String?) {
        if (error.isNullOrEmpty()) {
            binding.tvError.visibility = View.GONE
        } else {
            binding.tvError.visibility = View.VISIBLE
            binding.tvError.text = error
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
        val launchIntent = Intent(this, AddPdfActivity::class.java)
        val pageType = if (option.id == FROM_GALLERY) {
            AddPdfActivity.PageType.PickFromGallery
        } else {
            AddPdfActivity.PageType.DownloadPdf
        }
        launchIntent.putExtra(BundleArguments.ARGS_PAGE_TYPE, pageType.name)
        addPdfLauncher.launch(launchIntent)
    }

    private fun onPdfReaderActivityResult(result: ActivityResult?) {
        if (result != null && result.resultCode == RESULT_OK){
            if (result.data?.action == PdfReaderActivity.RESULT_ACTION_PDF_DELETED) {
                Alerts.successSnackBar(binding.root, "Pdf Deleted")
                // Refresh data
                viewModel.getAllPdfs()
            }
        }
    }

}