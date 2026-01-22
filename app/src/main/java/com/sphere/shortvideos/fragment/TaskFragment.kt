package com.sphere.shortvideos.fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import com.sphere.shortvideos.R
import com.sphere.shortvideos.baseui.GenericFragment
import com.sphere.shortvideos.databinding.FragmentTaskBinding
import com.sphere.shortvideos.databinding.ItemWatchCoinsBinding

/**
 * Date：2026/1/21
 * Describe:
 */
class TaskFragment : GenericFragment<FragmentTaskBinding>() {

    private val watchData = listOf(
        WatchItem("10min", "$1", true),
        WatchItem("15min", "$2", true),
        WatchItem("30min", "$3", false),
        WatchItem("60min", "$4", false),
        WatchItem("2h", "$5", false),
        WatchItem("4h", "$6", false)
    )

    override fun bindView(inflater: LayoutInflater,
                          container: ViewGroup?): FragmentTaskBinding {
        return FragmentTaskBinding.inflate(inflater, container, false)
    }

    override fun initUI() {
        setupWatchCoins()
    }

    private fun setupWatchCoins() {

    }

    data class WatchItem(
        val time: String,
        val reward: String,
        val isCompleted: Boolean
    )
}
