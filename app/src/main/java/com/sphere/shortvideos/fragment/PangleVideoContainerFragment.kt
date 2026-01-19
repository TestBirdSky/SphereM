package com.sphere.shortvideos.fragment

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bytedance.sdk.shortplay.api.PSSDK
import com.bytedance.sdk.shortplay.api.ShortPlay
import com.bytedance.sdk.shortplay.api.ShortPlayFragment
import com.sphere.shortvideos.GlobalConstants
import com.sphere.shortvideos.R
import com.sphere.shortvideos.activity.PangleDramaPlayActivity
import com.sphere.shortvideos.baseui.GenericFragment
import com.sphere.shortvideos.database
import com.sphere.shortvideos.database.DramaCollectEntity
import com.sphere.shortvideos.databinding.FragmentPangleVideoContainerBinding
import com.sphere.shortvideos.nextView
import com.sphere.shortvideos.showToast
import com.sphere.shortvideos.toJson
import com.ss.ttvideoengine.TTVideoEngineInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PangleVideoContainerFragment : GenericFragment<FragmentPangleVideoContainerBinding>() {

    private var shortPlay: ShortPlay? = null
    private var shortPlayCollect: DramaCollectEntity? = null
    private var detailFragment: ShortPlayFragment? = null
    private var onProgressChanged: (progress: Int, max: Int) -> Unit = { _, _ -> }

    companion object {
        fun newInstance(shortPlay: ShortPlay): PangleVideoContainerFragment {
            val args = Bundle().apply {
                putParcelable(GlobalConstants.EXTRA_KEY_SHORT_PLAY, shortPlay)
            }
            return PangleVideoContainerFragment().apply { arguments = args }
        }
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.run {
            shortPlay = getParcelable(GlobalConstants.EXTRA_KEY_SHORT_PLAY)
        }
    }

    override fun bindView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = run {
        FragmentPangleVideoContainerBinding.inflate(inflater, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun initUI() {
        initSeekbarAnim()
        lifecycleScope.launch(Dispatchers.Main) {
            shortPlay?.let { item ->
                withContext(Dispatchers.IO) {
                    shortPlayCollect = database.collectDao().getItemById(item.id.toString())
                }
                var isCollected = shortPlayCollect != null
                binding.imageCollect.imageTintList = ColorStateList.valueOf(if (isCollected) ContextCompat.getColor(requireContext(), R.color.color_red) else Color.WHITE)
                binding.seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) = Unit
                    override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        detailFragment?.setCurrentPlayTimeSeconds(seekBar?.progress ?: 0)
                    }
                })
                onProgressChanged = { progress, max ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (progress == 0 && max == 0) {
                            binding.seekbar.max = 0
                            binding.seekbar.isInvisible = true
                        } else if (binding.seekbar.max != max) {
                            binding.seekbar.max = max
                            binding.seekbar.isVisible = true
                        }
                        binding.seekbar.setProgress(progress, false)
                    }
                }
                binding.textName.text = item.title ?: ""
                binding.textPlayEp.text = "Ep.1 | Eps.${item.total}"
                binding.btnEp.setOnClickListener {
                    requireContext().nextView<PangleDramaPlayActivity> {
                        putExtra(GlobalConstants.EXTRA_KEY_SHORT_PLAY, item)
                        putExtra(GlobalConstants.EXTRA_KEY_START_INDEX, 1)
                        putExtra(GlobalConstants.EXTRA_KEY_START_PROGRESS, binding.seekbar.progress)
                        putExtra(GlobalConstants.EXTRA_KEY_COMMON_BOOLEAN, true)
                    }
                }
                binding.layoutPlayList.setOnClickListener {
                    requireContext().nextView<PangleDramaPlayActivity> {
                        putExtra(GlobalConstants.EXTRA_KEY_SHORT_PLAY, item)
                        putExtra(GlobalConstants.EXTRA_KEY_START_INDEX, 1)
                        putExtra(GlobalConstants.EXTRA_KEY_START_PROGRESS, binding.seekbar.progress)
                    }
                }
                binding.btnCollect.setOnClickListener {
                    isCollected = !isCollected
                    lifecycleScope.launch(Dispatchers.IO) {
                        if (isCollected) {
                            val newItem = DramaCollectEntity(item.toJson(), item.id.toString())
                            val uid = database.collectDao().upsert(newItem)
                            newItem.uid = uid
                            shortPlayCollect = newItem
                        } else {
                            shortPlayCollect?.let { database.collectDao().delete(it) }
                            shortPlayCollect = null
                        }
                        withContext(Dispatchers.Main) {
                            binding.imageCollect.imageTintList =
                                ColorStateList.valueOf(if (isCollected) ContextCompat.getColor(requireContext(), R.color.color_red) else Color.WHITE)
                        }
                    }
                }
                showDramaFragment(item)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initSeekbarAnim() {
        binding.seekbar.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> changeSeekbarSize(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> changeSeekbarSize(false)
            }
            false
        }
    }

    private fun changeSeekbarSize(isLarge: Boolean) {
        binding.seekbar.thumb = ContextCompat.getDrawable(requireContext(), if (isLarge) R.drawable.seekbar_thum_large else R.drawable.seekbar_thum)
        binding.seekbar.progressDrawable = ContextCompat.getDrawable(requireContext(), if (isLarge) R.drawable.seekbar_bg_large else R.drawable.seekbar_bg)
    }

    private fun showDramaFragment(shortPlay: ShortPlay) {
        val builder = PSSDK.DetailPageConfig.Builder()
        builder.hideLeftTopCloseAndTitle(true, PSSDK.ShortPlayDetailPageCloseListener {
            return@ShortPlayDetailPageCloseListener true
        }).apply {
            displayBottomExtraView(false)
            startPlayIndex(1)
            startPlayAtTimeSeconds(0)
            enableAutoPlayNext(false)
            displayProgressBar(false)
            playSingleItem(true)
            setVideoDisplayMode(TTVideoEngineInterface.IMAGE_LAYOUT_ASPECT_FILL)
            displayTextVisibility(PSSDK.DetailPageConfig.TEXT_POS_BOTTOM_TITLE, false)
            displayTextVisibility(PSSDK.DetailPageConfig.TEXT_POS_BOTTOM_DESC, false)
        }
        detailFragment = PSSDK.createDetailFragment(shortPlay, builder.build(), object : PSSDK.ShortPlayDetailPageListener {
            override fun onOverScroll(index: Int) = Unit
            override fun onProgressChange(shortPlay: ShortPlay?, index: Int, currentPlayTime: Int, duration: Int) {
                onProgressChanged.invoke(currentPlayTime, duration)
            }

            override fun onPlayFailed(e: PSSDK.ErrorInfo?): Boolean {
                return false
            }

            override fun onShortPlayPlayed(shortPlay: ShortPlay?, index: Int) = Unit

            @SuppressLint("SetTextI18n")
            override fun onItemSelected(position: Int, type: PSSDK.ShortPlayDetailPageListener.ItemType?, index: Int) = Unit

            override fun onVideoPlayStateChanged(shortPlay: ShortPlay?, index: Int, playbackState: Int) = Unit

            override fun onVideoPlayCompleted(shortPlay: ShortPlay?, index: Int) {
                requireContext().nextView<PangleDramaPlayActivity> {
                    putExtra(GlobalConstants.EXTRA_KEY_SHORT_PLAY, shortPlay)
                    putExtra(GlobalConstants.EXTRA_KEY_START_INDEX, 2)
                    putExtra(GlobalConstants.EXTRA_KEY_START_PROGRESS, 0)
                }
            }

            override fun onEnterImmersiveMode() = Unit
            override fun onExitImmersiveMode() = Unit

            override fun isNeedBlock(shortPlay: ShortPlay?, index: Int): Boolean = false

            override fun showAdIfNeed(
                shortPlay: ShortPlay?,
                p1: Int,
                p2: PSSDK.ShortPlayBlockResultListener?
            ) = Unit

            override fun onVideoInfoFetched(shortPlay: ShortPlay?, index: Int, videoPlayInfo: PSSDK.VideoPlayInfo?) = Unit
            override fun onObtainPlayerControlViews(): List<View?>? = null

        })
        if (detailFragment == null) {
            requireContext().showToast(R.string.play_failed_please_try_again)
            return
        }
        childFragmentManager.beginTransaction().add(R.id.fragment_container, detailFragment!!).show(detailFragment!!).commit()
    }

}