package com.stopsmoke.kekkek.presentation.test

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.stopsmoke.kekkek.R
import com.stopsmoke.kekkek.databinding.FragmentQuestionBinding
import com.stopsmoke.kekkek.invisible


class TestQuestionFragment : Fragment() {

    private var pageNum = 0 // 0 ~ 7 (총 8개의 질문)

    private var _binding: FragmentQuestionBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel by activityViewModels<TestViewModel>()

    private var score = 0

    private lateinit var testFragment: TestFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            pageNum = it.getInt(PAGE_NUM) // 지금 몇번째 화면인가?
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentQuestionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        testFragment = (parentFragment as TestFragment)
        setupView()
        setupListener()
    }

    private fun setupView() = with(binding) {
        tvQuestionMainText.text = getString(sharedViewModel.questionTitles[pageNum])
        btnQuestionAnswer1.text = getString(sharedViewModel.questionAnswers[pageNum][0])
        btnQuestionAnswer2.text = getString(sharedViewModel.questionAnswers[pageNum][1])
        btnQuestionAnswer3.text = getString(sharedViewModel.questionAnswers[pageNum][2])
        when (pageNum) {
            0 -> {
                progressbarQuestion.progress = 0
            }

            1 -> {
                progressbarQuestion.progress = 1
            }

            2 -> {
                progressbarQuestion.progress = 2
            }

            3 -> {
                progressbarQuestion.progress = 3
            }

            4 -> {
                progressbarQuestion.progress = 4
            }

            5 -> {
                progressbarQuestion.progress = 5
            }

            6 -> {
                progressbarQuestion.progress = 6
            }

            7 -> {
                progressbarQuestion.progress = 7
            }
        }
    }

    private fun setupListener() {
        with(binding) {
            btnQuestionAnswer1.setOnClickListener {
                score = 1
                sharedViewModel.addScore(score)
                testFragment.moveToNextQuestionPage()
            }
            btnQuestionAnswer2.setOnClickListener {
                score = 2
                sharedViewModel.addScore(score)
                testFragment.moveToNextQuestionPage()
            }
            btnQuestionAnswer3.setOnClickListener {
                score = 3
                sharedViewModel.addScore(score)
                testFragment.moveToNextQuestionPage()
            }

            includeQuestionAppBar.ivTestBack.setOnClickListener {
                activity?.onBackPressed()
            }
            includeQuestionAppBar.ivTestCancel.setOnClickListener {
//                ToastDialog()
                TestDialog().show(
                    parentFragmentManager,"금연 테스트"
                )
            }
        }
    }

    private fun ToastDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("금연 테스트")
        builder.setMessage("진행중인 금연 테스트를 종료하시겠습니까?")
        builder.setIcon(R.drawable.ic_smoke)

        val listener = DialogInterface.OnClickListener { _, type ->
            when (type) {
                DialogInterface.BUTTON_POSITIVE -> {
                    findNavController().popBackStack()
                    sharedViewModel.clearScore()
                }

                DialogInterface.BUTTON_NEGATIVE -> {}
            }
        }

        builder.setPositiveButton("예", listener)
        builder.setNegativeButton("아니요", listener)
        builder.show()
    }

    override fun onResume() {
        super.onResume()
        activity?.invisible()
    }

    companion object {
        private const val PAGE_NUM = "PAGE_NUMBER"

        fun newInstance(pageNum: Int): TestQuestionFragment {
            val fragment = TestQuestionFragment()
            val argument = Bundle().apply {
                putInt(PAGE_NUM, pageNum) // 새로운 페이 지의 페이지 번호를 전달함
            }
            fragment.arguments = argument
            return fragment
        }
    }
}