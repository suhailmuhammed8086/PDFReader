package com.example.pdfnotemate.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pdfnotemate.R
import com.example.pdfnotemate.databinding.AdapterBookmarkListBinding
import com.example.pdfnotemate.tools.DateTimeFormatter
import com.example.pdfnotemate.tools.pdf.viewer.model.BookmarkModel

class BookmarkListAdapter(
    private val context: Context,
    private val bookmarks: List<BookmarkModel>,
    private val listener: Listener,
) : RecyclerView.Adapter<BookmarkListAdapter.ViewHolder>() {
    var adapterState: AdapterState = AdapterState.IDLE
    enum class AdapterState{
        IDLE,
        SELECTION_MODE
    }
    interface Listener {
        fun onItemClicked(item: BookmarkModel, state: AdapterState, position: Int)
        fun onItemLongClicked(item: BookmarkModel, state: AdapterState, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkListAdapter.ViewHolder {
        return ViewHolder(AdapterBookmarkListBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: BookmarkListAdapter.ViewHolder, position: Int) {
        val item = bookmarks[position]
        holder.binding.apply {
            tvPage.text = context.getString(R.string.page).plus(item.page)
            tvUpdatedAt.text = DateTimeFormatter.format(item.updatedAt, DateTimeFormatter.DATE_AND_TIME_THREE)

            if (item.isSelected) {
                content.alpha = 0.5f
                ivSelected.visibility = View.VISIBLE
            } else {
                content.alpha = 1f
                ivSelected.visibility = View.GONE
            }

            root.setOnClickListener { listener.onItemClicked(item, adapterState, position) }
            root.setOnLongClickListener { listener.onItemLongClicked(item, adapterState, position);true }
        }
    }

    override fun getItemCount(): Int {
        return bookmarks.size
    }

    inner class ViewHolder(val binding: AdapterBookmarkListBinding) :
        RecyclerView.ViewHolder(binding.root) {}

}