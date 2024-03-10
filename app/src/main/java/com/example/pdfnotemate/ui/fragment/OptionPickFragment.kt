package com.example.pdfnotemate.ui.fragment

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pdfnotemate.R
import com.example.pdfnotemate.adapter.MoreOptionsAdapter
import com.example.pdfnotemate.databinding.FragmentOptionPickBinding
import com.example.pdfnotemate.utils.getParcelableArrayListVs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.parcelize.Parcelize


class OptionPickFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentOptionPickBinding
    private var adapter: MoreOptionsAdapter? = null
    private val options = ArrayList<MoreOptionModel>()
    private var title: String? = null
    private var listener: Listener? = null
    private var isFromFragment = false
    interface Listener {
        fun onMoreOptionSelected(option: MoreOptionModel)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isFromFragment = it.getBoolean(IS_FROM_FRAGMENT,false)
            title = it.getString(TITLE)
            options.addAll(it.getParcelableArrayListVs(OPTIONS,MoreOptionModel::class.java)?: emptyList())
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (!isFromFragment) {
            if (context is Listener) {
                listener = context
            }
        } else {
            onAttachToParent()
        }
    }

    private fun onAttachToParent(){
        parentFragment?.let {
            if (it is Listener) {
                listener = it
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentOptionPickBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvTitle.text = title?:""
        adapter = MoreOptionsAdapter(options.toList(), ::onOptionClicked)
        binding.rvOptions.apply {
            adapter = this@OptionPickFragment.adapter
            layoutManager = LinearLayoutManager(activity)
        }

        binding.btClose.setOnClickListener { dismiss() }
    }

    private fun onOptionClicked(selectedOption: MoreOptionModel){
        listener?.onMoreOptionSelected(selectedOption)
        dismiss()
    }

    companion object {
        const val IS_FROM_FRAGMENT = "IS_FROM_FRAGMENT"
        const val TITLE = "TITLE"
        const val OPTIONS = "OPTIONS"

        private const val ALERT_TAG = "MoreOption"
        fun show(manager: FragmentManager, title: String, options: List<MoreOptionModel>, isFromFragment:Boolean = false) {
            val instance = OptionPickFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(IS_FROM_FRAGMENT, isFromFragment)
                    putString(TITLE, title)
                    putParcelableArrayList(OPTIONS, ArrayList(options))
                }
            }

            (manager.findFragmentByTag(ALERT_TAG) as BottomSheetDialogFragment?)?.dismiss()

            instance.show(manager, ALERT_TAG)

        }

    }
}




@Parcelize
data class MoreOptionModel(
    val id: Int,
    val title: String,
): Parcelable