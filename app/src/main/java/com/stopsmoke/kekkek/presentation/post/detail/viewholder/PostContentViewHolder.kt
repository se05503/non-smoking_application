package com.stopsmoke.kekkek.presentation.post.detail.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.stopsmoke.kekkek.R
import com.stopsmoke.kekkek.core.domain.model.Post
import com.stopsmoke.kekkek.core.domain.model.ProfileImage
import com.stopsmoke.kekkek.core.domain.model.User
import com.stopsmoke.kekkek.databinding.RecyclerviewPostviewContentBinding
import com.stopsmoke.kekkek.presentation.post.detail.callback.PostCommentCallback
import com.stopsmoke.kekkek.presentation.post.detail.model.PostContentItem
import com.stopsmoke.kekkek.presentation.snackbarLongShow
import com.stopsmoke.kekkek.presentation.toResourceId
import com.stopsmoke.kekkek.presentation.mapper.toStringKR

class PostContentViewHolder(
    private val binding: RecyclerviewPostviewContentBinding,
    private val callback: PostCommentCallback?
) : RecyclerView.ViewHolder(binding.root) {

    init {
        initAdmob()
    }

    fun bind(headerItem: PostContentItem) {
        initUserProfileView(headerItem.user, headerItem.post)
        initPostView(headerItem.post)
        clickPostLike(headerItem.post)

        binding.ivPostPoster.setOnClickListener {
            headerItem.post?.let { post ->
                callback?.navigateToUserProfile(post.written.uid)
            }
        }
    }

    private fun initAdmob() {
        val adRequest = AdRequest.Builder().build()
        binding.adviewPost.loadAd(adRequest)
    }

    private fun clickPostLike(post: Post?) {
        binding.clPostViewHeart.setOnClickListener {
            if (post == null) {
                return@setOnClickListener
            }
            callback?.clickPostLike(post)
        }
    }

    private fun initUserProfileView(user: User?, post: Post?) {
        if (user == null || post == null) {
            return
        }

        when (user) {
            is User.Error -> {
                itemView.snackbarLongShow("유저 정보 에러")
            }

            is User.Guest -> {}
            is User.Registered -> {
                if (post.likeUser.contains(user.uid)) {
                    binding.ivPostHeart.setImageResource(R.drawable.ic_heart_filled)
                } else {
                    binding.ivPostHeart.setImageResource(R.drawable.ic_heart)
                }
            }
        }
    }

    private fun initPostView(post: Post?) = with(binding) {
        if (post == null) return@with

        tvPostPosterNickname.text = post.written.name
        tvPostPosterPostType.text = post.category.toStringKR()
        tvPostPosterRanking.text = "랭킹 ${post.written.ranking}위"
        tvPostHour.text = post.modifiedElapsedDateTime.toResourceId(itemView.context)

        tvPostTitle.text = post.title
        tvPostDescription.text = post.text
        tvPostHeartNum.text = post.likeUser.size.toString()
        tvPostCommentNum.text = post.commentCount.toString()
        tvPostViewNum.text = post.views.toString()
        initWrittenProfileImage(post.written.profileImage)
        binding.tvPostCommentNum.text = post.commentCount.toString()


        if (post.imagesUrl.isNotEmpty()) {
            binding.ivPostViewImage.load(post.imagesUrl[0])
            binding.cvPostViewImage.visibility = View.VISIBLE
        }
    }

    private fun initWrittenProfileImage(profileImage: ProfileImage) {
        when (profileImage) {
            is ProfileImage.Web -> binding.ivPostPoster.load(profileImage.url)
            is ProfileImage.Default -> binding.ivPostView.setImageResource(R.drawable.ic_user_profile_test)
        }
    }
}