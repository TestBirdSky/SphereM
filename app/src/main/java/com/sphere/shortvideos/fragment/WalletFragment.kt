package com.sphere.shortvideos.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import com.sphere.shortvideos.baseui.GenericFragment
import com.sphere.shortvideos.databinding.FragmentWallteBinding

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