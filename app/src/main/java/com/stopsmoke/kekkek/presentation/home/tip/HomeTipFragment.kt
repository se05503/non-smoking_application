package com.stopsmoke.kekkek.presentation.home.tip

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebViewClient
import androidx.navigation.fragment.findNavController
import com.stopsmoke.kekkek.databinding.FragmentHomeTipBinding
import com.stopsmoke.kekkek.databinding.FragmentSupportCenterBinding
import com.stopsmoke.kekkek.invisible
import com.stopsmoke.kekkek.visible

class HomeTipFragment : Fragment() {

    private var _binding: FragmentHomeTipBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeTipBinding.inflate(inflater, container, false)

        binding.includeHomeTipAppBar.ivHomeTipBack.setOnClickListener {
            findNavController().popBackStack()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding.webviewHometip) {
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            loadUrl("https://www.guro.go.kr/health/contents.do?key=1376")
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.invisible()
    }

    override fun onPause() {
        super.onPause()
        activity?.visible()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}