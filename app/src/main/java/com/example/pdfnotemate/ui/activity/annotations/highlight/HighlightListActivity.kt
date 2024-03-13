package com.example.pdfnotemate.ui.activity.annotations.highlight

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pdfnotemate.R
import com.example.pdfnotemate.adapter.HighlightListAdapter
import com.example.pdfnotemate.base.ui.BaseActivity
import com.example.pdfnotemate.databinding.ActivityHighlightListBinding
import com.example.pdfnotemate.model.DeleteAnnotationResponse
import com.example.pdfnotemate.state.ResponseState
import com.example.pdfnotemate.tools.pdf.viewer.model.HighlightModel
import com.example.pdfnotemate.utils.Alerts
import com.example.pdfnotemate.utils.BundleArguments
import com.example.pdfnotemate.utils.getParcelableArrayListExtraVs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HighlightListActivity : BaseActivity(), View.OnClickListener, HighlightListAdapter.Listener {
    private val viewModel : HighlightListViewModel by viewModels()
    private lateinit var binding : ActivityHighlightListBinding
    private var highlightListAdapter: HighlightListAdapter? = null
    private var highlights = arrayListOf<HighlightModel>()

    private var deletedHighlightIds = arrayListOf<Long>()


    companion object {
        const val RESULT_ACTION_OPEN_HIGHLIGHT = "action.open.highlight"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHighlightListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        getDataFromIntent()
        initView()

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                if (deletedHighlightIds.isEmpty()) {
                    finish()
                } else {
                    val result = Intent()
                    result.putExtra(BundleArguments.ARGS_DELETED_HIGHLIGHT_IDS,deletedHighlightIds.toLongArray())
                    setResult(RESULT_OK, result)
                    finish()
                }
            }
        })
    }



    private fun getDataFromIntent() {
        intent?.let {
            it.getParcelableArrayListExtraVs(BundleArguments.ARGS_HIGHLIGHTS, HighlightModel::class.java)?.also { highlights->
                this.highlights.clear()
                this.highlights.addAll(highlights)
            }
        }
    }

    private fun initView() {
        binding.btBack.setOnClickListener(this)
        binding.btDelete.setOnClickListener(this)
        highlightListAdapter = HighlightListAdapter(this,highlights, this)
        binding.rvHighLight.apply {
            adapter = highlightListAdapter
            layoutManager = LinearLayoutManager(this@HighlightListActivity)
        }

        checkItemCount()
    }

    private fun checkItemCount() {
        if (highlights.isEmpty()) {
            setError("No Highlight found")
        } else {
            setError(null)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btBack -> {
                if (highlightListAdapter?.adapterState == HighlightListAdapter.AdapterState.SELECTION_MODE) {
                    clearAllSelection()
                } else {
                    onBackPressedDispatcher.onBackPressed()
                }

            }

            R.id.btDelete -> {
                deleteHighlights()
            }
        }
    }

    private fun deleteHighlights() {
        val idsToDelete = highlights.filter { it.isSelected }.map { it.id }
        viewModel.deleteHighlights(idsToDelete)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun bindUI() = launch(Dispatchers.Main) {
        viewModel.deleteHighlightResponse.state.observe(this@HighlightListActivity) { state->
            when (state) {
                is ResponseState.Failed -> {
                    Alerts.failureSnackBar(binding.root,state.error)
                }
                ResponseState.Loading -> {}
                is ResponseState.Success<*> -> {
                    val response = state.response as DeleteAnnotationResponse?
                    if (response != null) {
                        deletedHighlightIds.addAll(response.deletedIds)
                        highlights.removeAll {
                            response.deletedIds.contains(it.id)
                        }
                        highlightListAdapter?.notifyDataSetChanged()
                        checkItemCount()
                    }
                }
                is ResponseState.ValidationError -> {}
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


    override fun onItemClicked(
        item: HighlightModel,
        state: HighlightListAdapter.AdapterState,
        position: Int
    ) {
       if (state == HighlightListAdapter.AdapterState.IDLE) {
           val result = Intent(RESULT_ACTION_OPEN_HIGHLIGHT).apply {
               putExtra(BundleArguments.ARGS_HIGHLIGHT, item)
               putExtra(BundleArguments.ARGS_DELETED_HIGHLIGHT_IDS, deletedHighlightIds.toLongArray())
           }
           setResult(RESULT_OK, result)
           finish()
       } else {
           highlights[position].isSelected = !highlights[position].isSelected
           highlightListAdapter?.notifyItemChanged(position)

           val selectedCount = highlights.count { it.isSelected }
           if (selectedCount == 0) {
               clearAllSelection()
           }
       }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onItemLongClicked(
        item: HighlightModel,
        state: HighlightListAdapter.AdapterState,
        position: Int
    ) {
        highlights[position].isSelected = true
        highlightListAdapter?.adapterState = HighlightListAdapter.AdapterState.SELECTION_MODE
        highlightListAdapter?.notifyDataSetChanged()
        setUpAppBar(HighlightListAdapter.AdapterState.SELECTION_MODE)
    }

    private fun setUpAppBar(state: HighlightListAdapter.AdapterState) {
        when (state) {
            HighlightListAdapter.AdapterState.IDLE ->  {
                binding.btBack.setImageResource(R.drawable.ic_arrow_back)
                binding.btDelete.visibility = View.GONE
            }
            HighlightListAdapter.AdapterState.SELECTION_MODE ->  {
                binding.btBack.setImageResource(R.drawable.ic_close_white)
                binding.btDelete.visibility = View.VISIBLE
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearAllSelection () {
        highlights.map { it.isSelected = false }
        highlightListAdapter?.adapterState = HighlightListAdapter.AdapterState.IDLE
        highlightListAdapter?.notifyDataSetChanged()
        setUpAppBar(HighlightListAdapter.AdapterState.IDLE)
    }

}