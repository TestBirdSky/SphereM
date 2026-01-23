package com.sphere.shortvideos.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.lifecycleScope
import com.esotericsoftware.spine.android.SpineController
import com.esotericsoftware.spine.android.SpineView
import com.sphere.shortvideos.baseui.GenericFragment
import com.sphere.shortvideos.databinding.FragmentWallteBinding
import com.sphere.shortvideos.view.SpineHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Date：2026/1/21
 * Describe:
 */
class WalletFragment : GenericFragment<FragmentWallteBinding>() {
    override fun bindView(inflater: LayoutInflater,
                          container: ViewGroup?): FragmentWallteBinding {
        return FragmentWallteBinding.inflate(inflater, container, false)
    }

    override fun initUI() {
    }
}