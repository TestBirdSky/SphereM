package com.sphere.shortvideos.dialogs

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bytedance.sdk.shortplay.api.ShortPlay
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayoutMediator
import com.sphere.shortvideos.R
import com.sphere.shortvideos.baseui.GenericBindActivity
import com.sphere.shortvideos.database.DramaEpisodeEntity
import com.sphere.shortvideos.databinding.DialogEpisodesBinding
import com.sphere.shortvideos.showToast

@SuppressLint("SetTextI18n")
fun GenericBindActivity<*>.showIndexSelectorDialog(
    shortPlay: ShortPlay,
    currentIndex: Int,
    episodeEntity: DramaEpisodeEntity,
    onChoose: (index: Int) -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val binding = DialogEpisodesBinding.inflate(LayoutInflater.from(this), window.decorView as ViewGroup, false)
    val dialog = BottomSheetDialog(this, R.style.TransparentMaterialDialog).apply {
        setContentView(binding.root)
        runCatching {
            findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        setCancelable(true)
        create()
    }
    dialog.setOnCancelListener {
        onCancel.invoke()
    }
    binding.textTitle.text = shortPlay.title ?: ""
    binding.textDesc.text = shortPlay.desc ?: ""
    binding.textTotalEp.text = "${shortPlay.total} ${getString(R.string.episodes)}(${getString(R.string.completed)})"
    var expanded = false
    binding.textDesc.setOnClickListener {
        if (expanded) {
            animateMaxLines(binding.textDesc, 3)
            binding.textDesc.ellipsize = TextUtils.TruncateAt.END
        } else {
            animateMaxLines(binding.textDesc, 10)
            binding.textDesc.ellipsize = TextUtils.TruncateAt.END
        }
        expanded = !expanded
    }
    val indexTabDatas = mutableListOf<IndexTabData>()
    val tabCount = if (shortPlay.total % 30 == 0) shortPlay.total / 30 else shortPlay.total / 30 + 1
    for (i in 0..<tabCount) {
        if (i == tabCount - 1) {
            indexTabDatas.add(IndexTabData(i * 30 + 1, shortPlay.total))
        } else {
            indexTabDatas.add(IndexTabData(i * 30 + 1, (i + 1) * 30))
        }
    }
    binding.viewPager.adapter = IndexTabListAdapter(this, indexTabDatas, currentIndex) { chooseIndex ->
        val highestIndex = episodeEntity.numbers.maxOrNull() ?: 1
        if (chooseIndex >= highestIndex + 2) {
            showToast(getString(R.string.watching_in_order))
            return@IndexTabListAdapter
        }
        onChoose.invoke(chooseIndex)
        dialog.dismiss()
        return@IndexTabListAdapter
    }
    TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
        val tabData = indexTabDatas[position]
        tab.text = "${tabData.startIndex}-${tabData.endIndex}"
    }.attach()
    runCatching {
        indexTabDatas.forEachIndexed { position, item ->
            if (currentIndex in item.startIndex..item.endIndex) {
                binding.viewPager.currentItem = position
                return@forEachIndexed
            }
        }
    }
    dialog.show()
}

fun animateMaxLines(textView: TextView, targetLines: Int) {
    val animator = ObjectAnimator.ofInt(textView, "maxLines", textView.maxLines, targetLines)
    animator.duration = 300
    animator.start()
}