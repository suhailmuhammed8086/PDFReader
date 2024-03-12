package com.example.pdfnotemate.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pdfnotemate.databinding.AdapterMoreOptionBinding
import com.example.pdfnotemate.databinding.AdapterTagBinding
import com.example.pdfnotemate.model.TagModel
import com.example.pdfnotemate.ui.fragment.MoreOptionModel

class TagsAdapter(
    private val tags: List<TagModel>,
    private val listener: Listener
) : RecyclerView.Adapter<TagsAdapter.ViewHolder>() {

    interface Listener{
        fun onTagClicked(tagModel: TagModel)
        fun onTagRemoveClicked(tagModel: TagModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            AdapterTagBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.tvTagName.text = tags[position].title
        holder.binding.tvTagName.setOnClickListener { listener.onTagClicked(tags[position]) }
        holder.binding.btRemoveTag.setOnClickListener { listener.onTagRemoveClicked(tags[position]) }
    }

    override fun getItemCount(): Int {
        return tags.size
    }

    inner class ViewHolder(val binding: AdapterTagBinding) :
        RecyclerView.ViewHolder(binding.root) {

    }
}