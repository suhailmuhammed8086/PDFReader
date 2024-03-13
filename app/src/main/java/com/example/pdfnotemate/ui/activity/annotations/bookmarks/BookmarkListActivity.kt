package com.example.pdfnotemate.ui.activity.annotations.bookmarks

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
import com.example.pdfnotemate.adapter.BookmarkListAdapter
import com.example.pdfnotemate.base.ui.BaseActivity
import com.example.pdfnotemate.databinding.ActivityBookmarkListBinding
import com.example.pdfnotemate.model.DeleteAnnotationResponse
import com.example.pdfnotemate.state.ResponseState
import com.example.pdfnotemate.tools.pdf.viewer.model.BookmarkModel
import com.example.pdfnotemate.utils.Alerts
import com.example.pdfnotemate.utils.BundleArguments
import com.example.pdfnotemate.utils.getParcelableArrayListExtraVs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BookmarkListActivity : BaseActivity(), View.OnClickListener, BookmarkListAdapter.Listener {
    private val viewModel : BookmarkListViewModel by viewModels()
    private lateinit var binding : ActivityBookmarkListBinding
    private var bookmarkListAdapter: BookmarkListAdapter? = null
    private var bookmarks = arrayListOf<BookmarkModel>()

    private var deletedBookmarksIds = arrayListOf<Long>()


    companion object {
        const val RESULT_ACTION_OPEN_BOOKMARK = "action.open.bookmark"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBookmarkListBinding.inflate(layoutInflater)
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
                if (deletedBookmarksIds.isEmpty()) {
                    finish()
                } else {
                    val result = Intent()
                    result.putExtra(BundleArguments.ARGS_DELETED_BOOKMARK_IDS,deletedBookmarksIds.toLongArray())
                    setResult(RESULT_OK, result)
                    finish()
                }
            }
        })
    }



    private fun getDataFromIntent() {
        intent?.let {
            it.getParcelableArrayListExtraVs(BundleArguments.ARGS_BOOKMARKS, BookmarkModel::class.java)?.also { bookmarks->
                this.bookmarks.clear()
                this.bookmarks.addAll(bookmarks)
            }
        }
    }

    private fun initView() {
        binding.btBack.setOnClickListener(this)
        binding.btDelete.setOnClickListener(this)
        bookmarkListAdapter = BookmarkListAdapter(this,bookmarks, this)
        binding.rvBookmarks.apply {
            adapter = bookmarkListAdapter
            layoutManager = LinearLayoutManager(this@BookmarkListActivity)
        }

        checkItemCount()
    }

    private fun checkItemCount() {
        if (bookmarks.isEmpty()) {
            setError("No Bookmarks found")
        } else {
            setError(null)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btBack -> {
                if (bookmarkListAdapter?.adapterState == BookmarkListAdapter.AdapterState.SELECTION_MODE) {
                    clearAllSelection()
                } else {
                    onBackPressedDispatcher.onBackPressed()
                }

            }

            R.id.btDelete -> {
                deleteBookmarks()
            }
        }
    }

    private fun deleteBookmarks() {
        val idsToDelete = bookmarks.filter { it.isSelected }.map { it.id }
        viewModel.deleteBookmarks(idsToDelete)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun bindUI() = launch(Dispatchers.Main) {
        viewModel.deleteBookmarksResponse.state.observe(this@BookmarkListActivity) { state->
            when (state) {
                is ResponseState.Failed -> {
                    Alerts.failureSnackBar(binding.root,state.error)
                }
                ResponseState.Loading -> {}
                is ResponseState.Success<*> -> {
                    val response = state.response as DeleteAnnotationResponse?
                    if (response != null) {
                        deletedBookmarksIds.addAll(response.deletedIds)
                        bookmarks.removeAll {
                            response.deletedIds.contains(it.id)
                        }
                        bookmarkListAdapter?.notifyDataSetChanged()
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
        item: BookmarkModel,
        state: BookmarkListAdapter.AdapterState,
        position: Int
    ) {
       if (state == BookmarkListAdapter.AdapterState.IDLE) {
           val result = Intent(RESULT_ACTION_OPEN_BOOKMARK).apply {
               putExtra(BundleArguments.ARGS_BOOKMARK, item)
               putExtra(BundleArguments.ARGS_DELETED_BOOKMARK_IDS, deletedBookmarksIds.toLongArray())
           }
           setResult(RESULT_OK, result)
           finish()
       } else {
           bookmarks[position].isSelected = !bookmarks[position].isSelected
           bookmarkListAdapter?.notifyItemChanged(position)

           val selectedCount = bookmarks.count { it.isSelected }
           if (selectedCount == 0) {
               clearAllSelection()
           }
       }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onItemLongClicked(
        item: BookmarkModel,
        state: BookmarkListAdapter.AdapterState,
        position: Int
    ) {
        bookmarks[position].isSelected = true
        bookmarkListAdapter?.adapterState = BookmarkListAdapter.AdapterState.SELECTION_MODE
        bookmarkListAdapter?.notifyDataSetChanged()
        setUpAppBar(BookmarkListAdapter.AdapterState.SELECTION_MODE)
    }

    private fun setUpAppBar(state: BookmarkListAdapter.AdapterState) {
        when (state) {
            BookmarkListAdapter.AdapterState.IDLE ->  {
                binding.btBack.setImageResource(R.drawable.ic_arrow_back)
                binding.btDelete.visibility = View.GONE
            }
            BookmarkListAdapter.AdapterState.SELECTION_MODE ->  {
                binding.btBack.setImageResource(R.drawable.ic_close_white)
                binding.btDelete.visibility = View.VISIBLE
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearAllSelection () {
        bookmarks.map { it.isSelected = false }
        bookmarkListAdapter?.adapterState = BookmarkListAdapter.AdapterState.IDLE
        bookmarkListAdapter?.notifyDataSetChanged()
        setUpAppBar(BookmarkListAdapter.AdapterState.IDLE)
    }

}