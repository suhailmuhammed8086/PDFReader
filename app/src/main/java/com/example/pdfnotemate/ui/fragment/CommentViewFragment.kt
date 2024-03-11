package com.example.pdfnotemate.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.example.pdfnotemate.R
import com.example.pdfnotemate.databinding.FragmentCommentViewBinding
import com.example.pdfnotemate.tools.pdf.viewer.model.CommentModel
import com.example.pdfnotemate.utils.Alerts
import com.example.pdfnotemate.utils.getParcelableVs
import com.example.pdfnotemate.utils.log
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CommentViewFragment : BottomSheetDialogFragment(), View.OnClickListener {
    private var pageType = PAGE_TYPE_VIEW
    private lateinit var binding: FragmentCommentViewBinding

    private var commentModel: CommentModel? = null

    private var listener: Listener? = null
    interface Listener{
        fun onDeleteCommentClick(commentId: Long)
        fun onEditCommentSaveClick(commentModel: CommentModel)
        fun onAddCommentSaveClick(commentModel: CommentModel)

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            commentModel = it.getParcelableVs(ARGS_COMMENT_MODEL,CommentModel::class.java)
            pageType = it.getInt(ARGS_PAGE_TYPE, PAGE_TYPE_VIEW)
        }
        commentModel.log("commentModel")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Listener) {
            listener = context
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpPageType()
        binding.btClose.setOnClickListener(this)
        binding.btDeleteNote.setOnClickListener(this)
        binding.btEditNote.setOnClickListener(this)
        binding.btSave.setOnClickListener(this)

        commentModel?.let {
            binding.tvSnippet.text = it.snippet
            binding.etComment.setText(it.text)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btClose -> dismiss()
            R.id.btDeleteNote -> {
                commentModel?.id?.let { listener?.onDeleteCommentClick(it) }
                dismiss()
            }

            R.id.btEditNote -> {
                pageType = PAGE_TYPE_EDIT
                setUpPageType()
            }

            R.id.btSave -> {
                submit()
                dismiss()
            }
        }
    }

    private fun submit() {
        val text = binding.etComment.text.toString().trim()
        if (text.isEmpty()) {
            Alerts.warningSnackBar(binding.root,"Please enter your comment")
            return
        }
        if (commentModel != null) {
            val mCommentModel = CommentModel(
                commentModel!!.id,
                commentModel!!.snippet,
                text,
                commentModel!!.page,
                0L,
                commentModel!!.coordinates
            )

            if (pageType == PAGE_TYPE_EDIT) {
                listener?.onEditCommentSaveClick(mCommentModel)
            } else if (pageType == PAGE_TYPE_ADD) {
                listener?.onAddCommentSaveClick(mCommentModel)
            }
        }

    }

    private fun setUpPageType() {
        when (pageType) {
            PAGE_TYPE_VIEW-> {
                binding.btDeleteNote.visibility = View.VISIBLE
                binding.btEditNote.visibility = View.VISIBLE
                binding.btSave.visibility = View.GONE
                binding.etComment.isEnabled = false
            }
            PAGE_TYPE_ADD-> {
                binding.btDeleteNote.visibility = View.GONE
                binding.btEditNote.visibility = View.GONE
                binding.btSave.visibility = View.VISIBLE
                binding.btSave.text = getString(R.string.save)
                binding.etComment.isEnabled = true
            }
            PAGE_TYPE_EDIT-> {
                binding.btDeleteNote.visibility = View.GONE
                binding.btEditNote.visibility = View.GONE
                binding.btSave.visibility = View.VISIBLE
                binding.btSave.text = getString(R.string.add)
                binding.etComment.isEnabled = true
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentCommentViewBinding.inflate(inflater,container, false)
        return binding.root
    }

    companion object {
        private const val PAGE_TYPE_VIEW = 1
        private const val PAGE_TYPE_ADD = 2
        private const val PAGE_TYPE_EDIT = 3

        private const val ARGS_COMMENT_MODEL = "ARGS_COMMENT_MODEL"
        private const val ARGS_PAGE_TYPE = "ARGS_PAGE_TYPE"

        private const val ALERT_TAG = "CommentAlert"


        private fun show(commentModel: CommentModel, manager: FragmentManager, pageType:Int) {
            (manager.findFragmentByTag(ALERT_TAG) as CommentViewFragment?)?.dismiss()
            val instance = CommentViewFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARGS_COMMENT_MODEL,commentModel)
                    putInt(ARGS_PAGE_TYPE, pageType)
                }
            }

            instance.show(manager, ALERT_TAG)
        }

        fun showToCommentView(commentModel: CommentModel, manager: FragmentManager){
            show(commentModel,manager, PAGE_TYPE_VIEW)
        }
        fun showToCommentEdit(commentModel: CommentModel, manager: FragmentManager){
            show(commentModel,manager, PAGE_TYPE_EDIT)
        }
        fun showToCommentAdd(commentModel: CommentModel, manager: FragmentManager){
            show(commentModel,manager, PAGE_TYPE_ADD)
        }
    }
}