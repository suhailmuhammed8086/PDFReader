package com.example.pdfnotemate.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pdfnotemate.R
import com.example.pdfnotemate.databinding.AdapterHighlightListBinding
import com.example.pdfnotemate.tools.DateTimeFormatter
import com.example.pdfnotemate.tools.pdf.viewer.model.HighlightModel

class HighlightListAdapter(
    private val context: Context,
    private val highlights: List<HighlightModel>,
    private val listener: Listener,
) : RecyclerView.Adapter<HighlightListAdapter.ViewHolder>() {
    var adapterState: AdapterState = AdapterState.IDLE
    enum class AdapterState{
        IDLE,
        SELECTION_MODE
    }
    interface Listener {
        fun onItemClicked(item: HighlightModel, state: AdapterState, position: Int)
        fun onItemLongClicked(item: HighlightModel, state: AdapterState, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HighlightListAdapter.ViewHolder {
        return ViewHolder(AdapterHighlightListBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: HighlightListAdapter.ViewHolder, position: Int) {
        val item = highlights[position]
        holder.binding.apply {
            tvPage.text = context.getString(R.string.page).plus(item.page)
            tvSnippet.text = item.snippet
            tvSnippet.setBackgroundColor(Color.parseColor(item.color.colorWithAlpha()))
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
        return highlights.size
    }

    inner class ViewHolder(val binding: AdapterHighlightListBinding) :
        RecyclerView.ViewHolder(binding.root) {}

    private fun String.colorWithAlpha(): String {
        return replace("#","#60")
    }
}