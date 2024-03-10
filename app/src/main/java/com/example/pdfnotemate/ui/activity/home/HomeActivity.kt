package com.example.pdfnotemate.ui.activity.home

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pdfnotemate.R
import com.example.pdfnotemate.adapter.PdfListAdapter
import com.example.pdfnotemate.base.ui.BaseActivity
import com.example.pdfnotemate.databinding.ActivityHomeBinding
import com.example.pdfnotemate.model.PdfNoteListModel
import com.example.pdfnotemate.model.PdfNotesResponse
import com.example.pdfnotemate.state.ResponseState
import com.example.pdfnotemate.ui.activity.add.AddPdfActivity
import com.example.pdfnotemate.ui.fragment.MoreOptionModel
import com.example.pdfnotemate.ui.fragment.OptionPickFragment
import com.example.pdfnotemate.utils.Alerts
import com.example.pdfnotemate.utils.BundleArguments
import com.example.pdfnotemate.utils.log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeActivity : BaseActivity(), View.OnClickListener, OptionPickFragment.Listener,
    PdfListAdapter.Listener {
    private lateinit var binding: ActivityHomeBinding
    private val viewModel: HomeViewModel by viewModels()


    private var adapter: PdfListAdapter? = null
    private var pdfList = arrayListOf<PdfNoteListModel>()

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
        adapter = PdfListAdapter(pdfList, this)
        binding.rvPdfList.apply {
            adapter = this@HomeActivity.adapter
            layoutManager = LinearLayoutManager(this@HomeActivity)
        }

    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btAddNewPdf -> {
                showAddOptions()
            }
        }
    }

    override fun onPdfItemClicked(pdf: PdfNoteListModel) {

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
                        pdfList.clear()
                        pdfList.addAll(pdfListResponse.notes)
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


}