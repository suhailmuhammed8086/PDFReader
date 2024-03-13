package com.example.pdfnotemate.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pdfnotemate.R
import com.example.pdfnotemate.databinding.AdapterCommentListBinding
import com.example.pdfnotemate.tools.DateTimeFormatter
import com.example.pdfnotemate.tools.pdf.viewer.model.CommentModel

class CommentListAdapter(
    private val context: Context,
    private val comments: List<CommentModel>,
    private val listener: Listener,
) : RecyclerView.Adapter<CommentListAdapter.ViewHolder>() {
    var adapterState: AdapterState = AdapterState.IDLE
    enum class AdapterState{
        IDLE,
        SELECTION_MODE
    }
    interface Listener {
        fun onItemClicked(item: CommentModel, state: AdapterState, position: Int)
        fun onItemLongClicked(item: CommentModel, state: AdapterState, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentListAdapter.ViewHolder {
        return ViewHolder(AdapterCommentListBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: CommentListAdapter.ViewHolder, position: Int) {
        val item = comments[position]
        holder.binding.apply {
            tvComment.text = item.text
            tvPage.text = context.getString(R.string.page).plus(item.page)
            tvSnippet.text = item.snippet
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
        return comments.size
    }

    inner class ViewHolder(val binding: AdapterCommentListBinding) :
        RecyclerView.ViewHolder(binding.root) {}
}