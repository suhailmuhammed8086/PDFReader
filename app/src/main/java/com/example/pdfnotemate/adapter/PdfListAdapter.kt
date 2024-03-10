package com.example.pdfnotemate.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.example.pdfnotemate.databinding.AdapterMoreOptionBinding
import com.example.pdfnotemate.databinding.AdapterPdfListBinding
import com.example.pdfnotemate.model.PdfNoteListModel
import com.example.pdfnotemate.ui.fragment.MoreOptionModel

class PdfListAdapter(
    private val pdfs: List<PdfNoteListModel>,
    private val listener: Listener
) : RecyclerView.Adapter<PdfListAdapter.ViewHolder>() {


    interface Listener {
        fun onPdfItemClicked(pdf: PdfNoteListModel)
        fun onPdfItemLongClicked(pdf: PdfNoteListModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            AdapterPdfListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = pdfs[position]
        holder.binding.tvTitle.text = item.title
        if (item.tag?.title.isNullOrEmpty()) {
            holder.binding.tvTag.visibility = View.GONE
        } else {
            holder.binding.tvTag.visibility = View.VISIBLE
            holder.binding.tvTag.text = item.tag?.title
        }
        holder.binding.tvHighlightsCount.text = item.highlights.toString().plus(" Highlights")
        holder.binding.tvCommentsCount.text = item.comments.toString().plus(" Comments")
        holder.binding.tvBookmarksCount.text = item.highlights.toString().plus(" Bookmarks")

        holder.binding.root.setOnClickListener { listener.onPdfItemClicked(item) }
        holder.binding.root.setOnLongClickListener { listener.onPdfItemClicked(item);true }

        if (position == pdfs.size-1) {
            (holder.binding.root.layoutParams as MarginLayoutParams?)?.bottomMargin = 200
        } else {
            (holder.binding.root.layoutParams as MarginLayoutParams?)?.bottomMargin = 0
        }
    }

    override fun getItemCount(): Int {
        return pdfs.size
    }

    inner class ViewHolder(val binding: AdapterPdfListBinding) :
        RecyclerView.ViewHolder(binding.root) {

    }
}