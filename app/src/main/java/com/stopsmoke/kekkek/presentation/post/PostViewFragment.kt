package com.stopsmoke.kekkek.presentation.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.stopsmoke.kekkek.R
import com.stopsmoke.kekkek.databinding.FragmentPostViewBinding
import com.stopsmoke.kekkek.databinding.FragmentPostViewBottomsheetDialogBinding
import com.stopsmoke.kekkek.domain.model.Comment
import com.stopsmoke.kekkek.domain.model.Post
import com.stopsmoke.kekkek.domain.model.Reply
import com.stopsmoke.kekkek.domain.model.User
import com.stopsmoke.kekkek.invisible
import com.stopsmoke.kekkek.presentation.CustomItemDecoration
import com.stopsmoke.kekkek.presentation.collectLatestWithLifecycle
import com.stopsmoke.kekkek.presentation.post.callback.PostCommentCallback
import com.stopsmoke.kekkek.presentation.post.callback.PostCommentDialogCallback
import com.stopsmoke.kekkek.presentation.post.dialog.DeleteDialogType
import com.stopsmoke.kekkek.presentation.post.dialog.PostCommentDeleteDialogFragment
import com.stopsmoke.kekkek.presentation.post.model.PostContentItem
import com.stopsmoke.kekkek.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PostViewFragment : Fragment(), PostCommentCallback, PostCommentDialogCallback {

    private var _binding: FragmentPostViewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostViewModel by viewModels()

    private lateinit var postViewAdapter: PostViewAdapter
    private lateinit var previewCommentAdapter: PreviewCommentAdapter
    private lateinit var postConcatAdapter: ConcatAdapter

    private val postDeleteDialog = lazy {
        AlertDialog.Builder(requireContext())
            .setTitle("게시글 삭제")
            .setMessage("게시글을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { dialog, _ ->
                viewModel.post.value?.id?.let { postId ->
                    viewModel.deletePost(postId)
                    Toast.makeText(requireContext(), "게시글이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }

    private val postActionDialog = lazy {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val bottomsheetDialogBinding =
            FragmentPostViewBottomsheetDialogBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(bottomsheetDialogBinding.root)

        bottomsheetDialogBinding.tvReportPost.setOnClickListener {
            findNavController().navigate(R.id.action_post_view_to_my_complaint)
            bottomSheetDialog.dismiss()
        }

        bottomsheetDialogBinding.tvDeletePost.setOnClickListener {
            if (viewModel.user.value !is User.Registered) return@setOnClickListener

            postDeleteDialog.value.show()
            bottomSheetDialog.dismiss()
        }

        bottomsheetDialogBinding.tvEditPost.setOnClickListener {
            if (viewModel.user.value !is User.Registered) return@setOnClickListener

            findNavController().navigate(
                resId = R.id.action_post_view_to_post_edit,
                args = bundleOf("post_id" to viewModel.postId.value)
            )
            bottomSheetDialog.dismiss()
        }

        (viewModel.user.value as? User.Registered)?.let { user ->
            if (viewModel.post.value?.written?.uid == user.uid) {
                bottomsheetDialogBinding.tvEditPost.visibility = View.VISIBLE
                bottomsheetDialogBinding.tvDeletePost.visibility = View.VISIBLE
            } else {
                bottomsheetDialogBinding.tvEditPost.visibility = View.GONE
                bottomsheetDialogBinding.tvDeletePost.visibility = View.GONE
            }
        }
        bottomSheetDialog
    }

    private lateinit var selectCommentId: String

    private val commentDeleteDialog = lazy {
        AlertDialog.Builder(requireContext())
            .setTitle("댓글 삭제")
            .setMessage("댓글을 삭제하시겠습니까?")
            .setPositiveButton("예") { dialog, _ ->
                viewModel.deleteComment(selectCommentId)
//                postViewAdapter.refresh()
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.getString("post_id", null)?.let {
            viewModel.updatePostId(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPostViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCommentRecyclerView()
        collectPostItem()
        observeCommentRecyclerViewItem()
        setupListener()
        autoScrollKeyboardWithRecyclerView()
        collectPreviewCommentItem()
    }

    private fun initCommentRecyclerView() {
        postViewAdapter = PostViewAdapter()
        previewCommentAdapter = PreviewCommentAdapter()
        postConcatAdapter = ConcatAdapter(postViewAdapter, previewCommentAdapter)
        binding.rvPostView.adapter = postConcatAdapter
        binding.rvPostView.layoutManager = LinearLayoutManager(requireContext())

        val color = ContextCompat.getColor(requireContext(), R.color.bg_thin_gray)
        val height = resources.getDimensionPixelSize(R.dimen.divider_height)
        binding.rvPostView.addItemDecoration(CustomItemDecoration(color, height))
    }

    private fun collectPostItem() {
        lifecycleScope.launch {
            combine(
                viewModel.user,
                viewModel.post,
                viewModel.commentCount
            ) { user: User?, post: Post?, l: Long? ->
                val headerItem = PostContentItem(user, post, l ?: 0)
                postViewAdapter.updatePostHeader(headerItem)

                if (user !is User.Registered) return@combine
                if (post == null) return@combine

                if (post.bookmarkUser.contains(user.uid)) {
                    binding.includePostViewAppBar.ivPostBookmark.setImageResource(R.drawable.ic_bookmark_filled)
                } else {
                    binding.includePostViewAppBar.ivPostBookmark.setImageResource(R.drawable.ic_bookmark)
                }
            }
                .flowWithLifecycle(lifecycle)
                .collect()
        }
    }

    private fun observeCommentRecyclerViewItem() {
        viewModel.comment.collectLatestWithLifecycle(lifecycle) {
            postViewAdapter.submitData(it)
        }
    }

    private fun showCommentDeleteDialog(commentId: String) {
        val commentDeleteDialog = PostCommentDeleteDialogFragment(
            this@PostViewFragment,
            DeleteDialogType.CommentDeleteDialog(commentId)
        )
        commentDeleteDialog.show(childFragmentManager, "commentDeleteDialog")
    }

    private fun autoScrollKeyboardWithRecyclerView() {
        binding.rvPostView.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom < oldBottom) {
                binding.rvPostView.scrollBy(0, oldBottom - bottom)
            }
        }
    }

    private fun collectPreviewCommentItem() {
        viewModel.previewCommentItem.collectLatestWithLifecycle(lifecycle) {
            previewCommentAdapter.submitList(it)

            if (it.isNotEmpty()) {
                binding.rvPostView.smoothScrollToPosition(postConcatAdapter.itemCount)
            }
        }
    }

    private fun setupListener() = with(binding) {
        includePostViewAppBar.ivPostBack.setOnClickListener {
            findNavController().popBackStack()
        }

        btnPostAddComment.setOnClickListener {
            val comment = etPostAddComment.text.toString()
            if (comment.isEmpty()) {
                Toast.makeText(requireContext(), "댓글을 입력해주세요!", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.addComment(text = comment)
                binding.etPostAddComment.setText("")
                binding.root.hideSoftKeyboard()
            }
        }

        includePostViewAppBar.ivPostBookmark.setOnClickListener {
            viewModel.toggleBookmark()
        }

        includePostViewAppBar.ivPostMore.setOnClickListener {
            postActionDialog.value.show()
        }

        postViewAdapter.registerCallback(this@PostViewFragment)
    }

    override fun onResume() {
        super.onResume()
        activity?.invisible()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.visible()
        _binding = null
        dismissPostDeleteDialog()
        dismissCommentDeleteDialog()
        dismissPostActionDialog()
    }

    private fun dismissPostDeleteDialog() {
        if (postDeleteDialog.isInitialized()) {
            postDeleteDialog.value.dismiss()
        }
    }

    private fun dismissCommentDeleteDialog() {
        if (commentDeleteDialog.isInitialized()) {
            commentDeleteDialog.value.dismiss()
        }
    }

    private fun dismissPostActionDialog() {
        if (postActionDialog.isInitialized()) {
            postActionDialog.value.dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        postViewAdapter.unregisterCallback()
    }

    private fun View.hideSoftKeyboard() {
        val inputMethodManager = getSystemService(context, InputMethodManager::class.java)
        inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)
    }

    override fun deleteItem(comment: Comment) {
        val user = viewModel.user.value as? User.Registered ?: return

        if (comment.written.uid == user.uid) {
            selectCommentId = comment.id
            showCommentDeleteDialog(comment.id)
        }
    }

    override fun navigateToUserProfile(uid: String) {
        findNavController().navigate(
            resId = R.id.action_post_view_to_user_profile,
            args = bundleOf("uid" to uid)
        )
    }

    override fun commentLikeClick(comment: Comment) {
        viewModel.toggleCommentLike(comment)
    }

    override fun clickPostLike(post: Post) {
        viewModel.toggleLikeToPost()
    }

    override fun clickReplyLike(reply: Reply) {
        viewModel.toggleReplyLike(reply)
    }

    override fun navigateToReply(comment: Comment) {
        findNavController().navigate(
            resId = R.id.action_post_view_to_reply,
            args = bundleOf(
                "post_id" to comment.parent.postId,
                "comment_id" to comment.id
            )
        )
    }

    //PostCommentDialogCallback
    override fun deleteComment(commentId: String) {
        viewModel.deleteComment(commentId)
    }

    override fun deletePost(postId: String) {
        viewModel.deletePost(postId)
        Toast.makeText(requireContext(), "게시글이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }
}