package com.stopsmoke.kekkek.presentation.home.rankingList.rankinglistfield

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.stopsmoke.kekkek.R
import com.stopsmoke.kekkek.databinding.FragmentRankingListFieldBinding
import com.stopsmoke.kekkek.domain.model.User
import com.stopsmoke.kekkek.presentation.collectLatestWithLifecycle
import com.stopsmoke.kekkek.presentation.home.HomeViewModel
import com.stopsmoke.kekkek.presentation.home.rankingList.RankingListAdapter
import com.stopsmoke.kekkek.presentation.home.rankingList.RankingListCallback
import com.stopsmoke.kekkek.presentation.home.rankingList.RankingListItem
import com.stopsmoke.kekkek.presentation.home.rankingList.toRankingListItem
import dagger.hilt.android.AndroidEntryPoint
import java.time.Duration
import java.time.LocalDateTime

@AndroidEntryPoint
class RankingListFieldFragment : Fragment() {
    private var _binding: FragmentRankingListFieldBinding? = null
    private val binding get() = _binding!!

    private var field: RankingListField? = null

    private val viewModel: HomeViewModel by activityViewModels()

    private lateinit var listAdapter: RankingListAdapter

    private var callback: RankingListCallback? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (parentFragment is RankingListCallback)
            callback = parentFragment as RankingListCallback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            field = it.getParcelable<RankingListField>(FIELD)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRankingListFieldBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initViewModel()
        initListener()
    }

    private fun initView() = with(binding) {
        when (field) {
            RankingListField.Time -> {

                tvRankingListTypeTime.apply {
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_blue))
                    setBackgroundResource(R.drawable.bg_rankinglist_ranktype)
                }

                tvRankingListTypeAchievement.apply {
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_lightgray2))
                    setBackgroundColor(0)
                }


            }

            RankingListField.Achievement -> {
                tvRankingListTypeAchievement.apply {
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_blue))
                    setBackgroundResource(R.drawable.bg_rankinglist_ranktype)
                }

                tvRankingListTypeTime.apply {
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_lightgray2))
                    setBackgroundColor(0)
                }
            }

            null -> {

            }
        }

        field?.let { listAdapter = RankingListAdapter(it) }
        callback?.let { listAdapter.registerCallbackListener(it) }
        rvRankingList.adapter = listAdapter
        rvRankingList.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun initListener() = with(binding) {
        tvRankingListTypeTime.setOnClickListener {
            callback?.replaceFragment(RankingListFieldFragment.newInstance(RankingListField.Time))
        }

        tvRankingListTypeAchievement.setOnClickListener {
            callback?.replaceFragment(RankingListFieldFragment.newInstance(RankingListField.Achievement))
        }
    }

    private fun initViewModel() = with(viewModel) {
        userRankingList.collectLatestWithLifecycle(lifecycle) {
            when (field) {
                RankingListField.Time -> {
                    val list = it.filter { it.startTime != null }.sortedBy { it.startTime }
                    listAdapter.submitList(list)

                    initTopRank(list)

                    (viewModel.user.value as? User.Registered)?.toRankingListItem()?.let { user ->
                        binding.includeRankingListMyRank.apply {
                            tvRankStateItemRankNum.text = (list.indexOf(user) + 1).toString()
                            tvRankStateItem.text = user.introduction
                            tvRankStateItemUserName.text = user.name

                            if (user.profileImage.isNullOrEmpty()) {
                                circleIvRankStateItemProfile.setImageResource(
                                    R.drawable.img_defaultprofile
                                )
                            } else {
                                circleIvRankStateItemProfile.load(user.profileImage)
                            }

                            tvRankingListTime.text = getRankingTime(user.startTime!!)

                            circleIvRankStateItemProfile.setOnClickListener {
                                callback?.navigationToUserProfile(user.userID)
                            }
                        }
                    }
                }

                RankingListField.Achievement -> {
                    val list = it.sortedByDescending { it.clearAchievementInt }
                    listAdapter.submitList(list)

                    initTopRank(list)

                    (viewModel.user.value as? User.Registered)?.toRankingListItem()?.let { user ->
                        binding.includeRankingListMyRank.apply {
                            tvRankStateItemRankNum.text = (list.indexOf(user) + 1).toString()
                            tvRankStateItem.text = user.introduction
                            tvRankStateItemUserName.text = user.name

                            if (user.profileImage.isNullOrEmpty()) {
                                circleIvRankStateItemProfile.setImageResource(
                                    R.drawable.img_defaultprofile
                                )
                            } else {
                                circleIvRankStateItemProfile.load(user.profileImage)
                            }

                            tvRankingListTime.text = user.clearAchievementInt.toString() + "개"

                            circleIvRankStateItemProfile.setOnClickListener {
                                callback?.navigationToUserProfile(user.userID)
                            }
                        }
                    }
                }

                null -> {}
            }
        }
    }

    private fun initTopRank(list: List<RankingListItem>) = with(binding) {
        val topRankTextList = listOf(
            listOf(tvRankingListRank1Name, tvRankingListRank1Intro),
            listOf(tvRankingListRank2Name, tvRankingListRank2Intro),
            listOf(tvRankingListRank3Name, tvRankingListRank3Intro)
        )

        val topRankProfileList = listOf(
            ivRankingListRank1Profile,
            ivRankingListRank2Profile,
            ivRankingListRank3Profile
        )

        for (i in topRankTextList.indices) {
            topRankTextList[i][0].text = list[i].name
            topRankTextList[i][1].text = list[i].introduction

            if (list[i].profileImage.isNullOrEmpty()) {
                topRankProfileList[i].setImageResource(
                    R.drawable.img_defaultprofile
                )
            } else {
                topRankProfileList[i].load(list[i].profileImage)
            }
        }
    }

    private fun getRankingTime(startTime: LocalDateTime): String { //-일 -시간
        val currentTime = LocalDateTime.now()
        val duration = Duration.between(startTime, currentTime)

        val days = duration.toDays()
        val hours = duration.toHours() % 24


        return "${days}일 ${hours}시간"
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        callback = null
    }

    companion object {
        private const val FIELD = "field"

        fun newInstance(rankingListField: RankingListField) =
            RankingListFieldFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(FIELD, rankingListField)
                }
            }
    }
}