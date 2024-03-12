package com.example.pdfnotemate.ui.fragment.tag

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.example.pdfnotemate.R
import com.example.pdfnotemate.adapter.TagsAdapter
import com.example.pdfnotemate.base.ui.ScopedBottomSheetFragment
import com.example.pdfnotemate.databinding.FragmentTagBinding
import com.example.pdfnotemate.model.RemoveTagResponse
import com.example.pdfnotemate.model.TagModel
import com.example.pdfnotemate.state.ResponseState
import com.example.pdfnotemate.utils.Alerts
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@AndroidEntryPoint
class TagFragment : ScopedBottomSheetFragment(), View.OnClickListener, TagsAdapter.Listener {
    private lateinit var binding: FragmentTagBinding
    private val viewModel: TagViewModel by viewModels()
    private var listener: Listener? = null
    private var tagAdapter: TagsAdapter?= null
    private var tags = arrayListOf<TagModel>()


    interface Listener {
        fun onTagSelected(tagModel: TagModel)
        fun onTagRemoved(tagId: Long)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Listener) {
            listener = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentTagBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btClose.setOnClickListener(this)
        binding.btAddTag.setOnClickListener(this)

        tagAdapter = TagsAdapter(tags, this)
        binding.rvTags.apply {
            adapter = tagAdapter
            layoutManager = FlexboxLayoutManager(activity, FlexDirection.ROW)
        }

        viewModel.loadAllTags()
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btClose -> {
                dismiss()
            }
            R.id.btAddTag -> {
                val title = binding.etTagName.text.toString().trim()
                if (title.isEmpty()) {
                    Alerts.warningSnackBar(binding.root,"Enter tag name")
                    return
                }
                viewModel.addTag(title)
            }
        }
    }

    override fun onTagClicked(tagModel: TagModel) {
        listener?.onTagSelected(tagModel)
        dismiss()
    }

    override fun onTagRemoveClicked(tagModel: TagModel) {
        viewModel.removeTag(tagModel.id)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun bindUI() = launch (Dispatchers.Main){
        viewModel.tagListResponse.state.observe(this@TagFragment) {state->
            when (state) {
                is ResponseState.Failed -> {
                    Alerts.failureSnackBar(binding.root,state.error)
                }
                ResponseState.Loading -> {}
                is ResponseState.Success<*> -> {
                    val response = (state.response as List<*>?)?.map { it as TagModel }
                    if (response != null) {
                        tags.clear()
                        tags.addAll(response)
                    }
                    tagAdapter?.notifyDataSetChanged()

                }
                is ResponseState.ValidationError -> {
                    Alerts.warningSnackBar(binding.root, state.error)
                }
            }
        }
        viewModel.addTagResponse.state.observe(this@TagFragment) {state->
            when (state) {
                is ResponseState.Failed -> {
                    Alerts.failureSnackBar(binding.root,state.error)
                }
                ResponseState.Loading -> {}
                is ResponseState.Success<*> -> {
                    val response = state.response as TagModel?
                    if (response != null){
                        tags.add(response)
                        tagAdapter?.notifyDataSetChanged()
                        binding.etTagName.setText("")
                    }
                }
                is ResponseState.ValidationError -> {
                    Alerts.warningSnackBar(binding.root, state.error)
                }
            }
        }
        viewModel.removeTagResponse.state.observe(this@TagFragment) {state->
            when (state) {
                is ResponseState.Failed -> {
                    Alerts.failureSnackBar(binding.root,state.error)
                }
                ResponseState.Loading -> {}
                is ResponseState.Success<*> -> {
                    val response = state.response as RemoveTagResponse?
                    if (response != null){
                        tags.removeAll { it.id == response.tagId }
                        tagAdapter?.notifyDataSetChanged()
                        listener?.onTagRemoved(response.tagId)
                    }
                }
                is ResponseState.ValidationError -> {
                    Alerts.warningSnackBar(binding.root, state.error)
                }
            }
        }
    }

    companion object {

        private const val ALERT_TAG = "tagDialog"
        @JvmStatic
        fun show(manager: FragmentManager) {
            (manager.findFragmentByTag(ALERT_TAG) as TagFragment?)?.dismiss()

            val instance = TagFragment()
            instance.show(manager, ALERT_TAG)
        }

    }
}