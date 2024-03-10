package com.example.pdfnotemate.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pdfnotemate.databinding.AdapterMoreOptionBinding
import com.example.pdfnotemate.ui.fragment.MoreOptionModel

class MoreOptionsAdapter(
    private val moreOptions: List<MoreOptionModel>,
    private val onOptionClicked: (option: MoreOptionModel) -> Unit
) : RecyclerView.Adapter<MoreOptionsAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            AdapterMoreOptionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.tvOption.text = moreOptions[position].title
        holder.binding.tvOption.setOnClickListener { onOptionClicked(moreOptions[position]) }
    }

    override fun getItemCount(): Int {
        return moreOptions.size
    }

    inner class ViewHolder(val binding: AdapterMoreOptionBinding) :
        RecyclerView.ViewHolder(binding.root) {

    }
}