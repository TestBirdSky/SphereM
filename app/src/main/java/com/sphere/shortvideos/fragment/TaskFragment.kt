package com.sphere.shortvideos.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import com.sphere.shortvideos.baseui.GenericFragment
import com.sphere.shortvideos.databinding.FragmentTaskBinding

/**
 * Date：2026/1/21
 * Describe:
 */
class TaskFragment : GenericFragment<FragmentTaskBinding>() {
    override fun bindView(inflater: LayoutInflater,
                          container: ViewGroup?): FragmentTaskBinding {
        return FragmentTaskBinding.inflate(inflater, container, false)
    }

    override fun initUI() {

    }
}