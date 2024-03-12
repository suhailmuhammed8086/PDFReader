package com.example.pdfnotemate.ui.activity.reader

import android.graphics.PointF
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.pdfnotemate.R
import com.example.pdfnotemate.base.ui.BaseActivity
import com.example.pdfnotemate.databinding.ActivityPdfReaderBinding
import com.example.pdfnotemate.databinding.ContainerPdfReaderBinding
import com.example.pdfnotemate.model.AnnotationListResponse
import com.example.pdfnotemate.model.PdfNoteListModel
import com.example.pdfnotemate.state.ResponseState
import com.example.pdfnotemate.tools.pdf.viewer.PDFView
import com.example.pdfnotemate.tools.pdf.viewer.PdfFile
import com.example.pdfnotemate.tools.pdf.viewer.model.CommentModel
import com.example.pdfnotemate.tools.pdf.viewer.model.Coordinates
import com.example.pdfnotemate.tools.pdf.viewer.model.HighlightModel
import com.example.pdfnotemate.tools.pdf.viewer.model.PdfAnnotationModel
import com.example.pdfnotemate.tools.pdf.viewer.model.TextSelectionData
import com.example.pdfnotemate.tools.pdf.viewer.selection.TextSelectionOptionsWindow
import com.example.pdfnotemate.ui.fragment.CommentViewFragment
import com.example.pdfnotemate.ui.fragment.MoreOptionModel
import com.example.pdfnotemate.ui.fragment.OptionPickFragment
import com.example.pdfnotemate.utils.BundleArguments
import com.example.pdfnotemate.utils.getParcelableExtraVs
import com.example.pdfnotemate.utils.log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

@AndroidEntryPoint
class PdfReaderActivity : BaseActivity(), View.OnClickListener, OptionPickFragment.Listener, CommentViewFragment.Listener {
    private lateinit var binding: ActivityPdfReaderBinding
    private lateinit var contentBinding: ContainerPdfReaderBinding

    // Popup window that show after user selecting a text in pdf
    private var textSelectionOptionWindow: TextSelectionOptionsWindow? = null
    private var pdfRenderScope: CoroutineScope = CoroutineScope(Dispatchers.IO)



    private val viewModel: PdfReaderViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPdfReaderBinding.inflate(layoutInflater)
        contentBinding = binding.container
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        getDataFromIntent()
        initView()
        preparePdfView()
    }

    private fun getDataFromIntent() {
        viewModel.pdfDetails = intent?.getParcelableExtraVs(BundleArguments.ARGS_PDF_DETAILS, PdfNoteListModel::class.java)
        viewModel.pdfDetails.log("pdfDetails")
    }

    private fun preparePdfView() {
        contentBinding.pdfView
            .attachCoroutineScope(pdfRenderScope)
            .setListener(pdfCallBack)
        textSelectionOptionWindow = TextSelectionOptionsWindow(this, textSelectionWindowCallback)
        textSelectionOptionWindow?.attachToPdfView(contentBinding.pdfView)
        loadPdf()

    }

    private fun loadPdf() {
        if (!viewModel.pdfDetails?.filePath.isNullOrEmpty()) {
            val file = File(viewModel.pdfDetails?.filePath!!)
            contentBinding.pdfView
                .fromFile(file)
                .defaultPage(0)
                .load()
        } else {
            Toast.makeText(this, "pdf file not found", Toast.LENGTH_SHORT).show()
            finish()
        }

    }

    override fun bindUI() = launch(Dispatchers.Main) {
        viewModel.addCommentResponse.state.observe(this@PdfReaderActivity){state->
            when(state) {
                is ResponseState.Failed -> {

                }
                ResponseState.Loading -> {

                }
                is ResponseState.Success<*> -> {
                    val response = state.response as CommentModel?
                    if (response!= null) {
                        viewModel.annotations.comments.add(0,response)
                        contentBinding.pdfView.addComment(response)
                    }
                }
                is ResponseState.ValidationError -> {

                }
            }
        }
        viewModel.addHighlightResponse.state.observe(this@PdfReaderActivity){state->
            when(state) {
                is ResponseState.Failed -> {

                }
                ResponseState.Loading -> {

                }
                is ResponseState.Success<*> -> {
                    val response = state.response as HighlightModel?
                    if (response!= null) {
                        viewModel.annotations.highlights.add(0,response)
                        contentBinding.pdfView.addHighlight(response)
                    }
                }
                is ResponseState.ValidationError -> {

                }
            }
        }
        viewModel.addBookmarkResponse.state.observe(this@PdfReaderActivity){state->
            when(state) {
                is ResponseState.Failed -> {

                }
                ResponseState.Loading -> {

                }
                is ResponseState.Success<*> -> {

                }
                is ResponseState.ValidationError -> {

                }
            }
        }
        viewModel.annotationListResponse.state.observe(this@PdfReaderActivity){state->
            when(state) {
                is ResponseState.Failed -> {

                }
                ResponseState.Loading -> {

                }
                is ResponseState.Success<*> -> {
                    val response = state.response as AnnotationListResponse?
                    if (response != null) {
                        viewModel.annotations = response

                        val highlightAndComments = arrayListOf<PdfAnnotationModel>()
                        highlightAndComments.addAll(response.comments)
                        highlightAndComments.addAll(response.highlights)
                        contentBinding.pdfView.loadAnnotations(highlightAndComments)
                    }
                }
                is ResponseState.ValidationError -> {

                }
            }
        }
    }
    private val pdfCallBack = object : PDFView.Listener{

        override fun onPreparationStarted() {
            contentBinding.progressBar.visibility = View.VISIBLE
        }

        override fun onPreparationSuccess() {
            viewModel.loadAllAnnotations()

            contentBinding.progressBar.visibility = View.GONE
        }

        override fun onPreparationFailed(error: String, e: Exception?) {
            contentBinding.progressBar.visibility = View.GONE
        }

        override fun onPageChanged(pageIndex: Int, paginationPageIndex: Int) {

        }

        override fun onTextSelected(selection: TextSelectionData, rawPoint: PointF) {
            textSelectionOptionWindow?.show(rawPoint.x, rawPoint.y, selection)
        }

        override fun hideTextSelectionOptionWindow() {
            textSelectionOptionWindow?.dismiss()
        }

        override fun onTextSelectionCleared() {

        }

        override fun onNotesStampsClicked(notes: List<CommentModel>, pointOfNote: PointF) {

        }

        override fun loadTopPdfChunk(mergeId: Int, pageIndexToLoad: Int) {

        }

        override fun loadBottomPdfChunk(mergedId: Int, pageIndexToLoad: Int) {

        }

        override fun onScrolling() {

        }

        override fun onTap() {

        }

        override fun onMergeStart(mergeId: Int, mergeType: PdfFile.MergeType) {

        }

        override fun onMergeEnd(mergeId: Int, mergeType: PdfFile.MergeType) {

        }

        override fun onMergeFailed(
            mergeId: Int,
            mergeType: PdfFile.MergeType,
            message: String,
            exception: java.lang.Exception?
        ) {

        }


    }

    private val textSelectionWindowCallback = object :  TextSelectionOptionsWindow.Listener {
        override fun onAddHighlightClick(
            snippet: String,
            color: String,
            page: Int,
            coordinates: Coordinates
        ) {
            viewModel.addHighlight(snippet,color, page, coordinates)
        }

        override fun onAddNotClick(snippet: String, page: Int, coordinates: Coordinates) {
            val commentModel = CommentModel(
                -1, snippet, "", page, 0L, coordinates
            )
            CommentViewFragment.showToCommentAdd(commentModel, supportFragmentManager)
        }
    }


        private fun initView() {
        binding.btBack.setOnClickListener(this)
        binding.btMoreOptions.setOnClickListener(this)
        binding.tvTitle.text = viewModel.pdfDetails?.title ?: ""
    }
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btBack -> {
                finish()
            }
            R.id.btMoreOptions -> {
                showMoreOptions()
            }
        }
    }

    private fun showMoreOptions() {
        val options = listOf(
            MoreOptionModel(1, "Highlights"),
            MoreOptionModel(2, "Comments"),
            MoreOptionModel(3, "Bookmarks"),
        )
        OptionPickFragment.show(
            supportFragmentManager,
            viewModel.pdfDetails?.title ?: "",
            options
        )
    }

    override fun onMoreOptionSelected(option: MoreOptionModel) {

    }

    override fun onDeleteCommentClick(commentId: Long) {

    }

    override fun onEditCommentSaveClick(commentModel: CommentModel) {

    }

    override fun onAddCommentSaveClick(commentModel: CommentModel) {
        textSelectionOptionWindow?.dismiss(true)
        viewModel.addComment(commentModel.snippet,commentModel.text,commentModel.page,commentModel.coordinates)
    }
}