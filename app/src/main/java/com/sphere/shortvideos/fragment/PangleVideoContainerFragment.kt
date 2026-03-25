package com.sphere.shortvideos.fragment

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.bytedance.sdk.shortplay.api.EpisodeData
import com.bytedance.sdk.shortplay.api.PSSDK
import com.bytedance.sdk.shortplay.api.ShortPlay
import com.bytedance.sdk.shortplay.api.ShortPlayFragment
import com.chartboost.sdk.impl.fa
import com.sphere.shortvideos.GlobalConstants
import com.sphere.shortvideos.R
import com.sphere.shortvideos.activity.MainActivity
import com.sphere.shortvideos.activity.PangleDramaPlayActivity
import com.sphere.shortvideos.baseui.GenericFragment
import com.sphere.shortvideos.database
import com.sphere.shortvideos.database.DramaCollectEntity
import com.sphere.shortvideos.databinding.FragmentPangleVideoContainerBinding
import com.sphere.shortvideos.dialogs.WelcomeBonusDialogFragment
import com.sphere.shortvideos.helper.HelperRewardShow
import com.sphere.shortvideos.helper.HelperRewardShow.isShowCanCash
import com.sphere.shortvideos.helper.HelperRewardShow.showBubbleTips
import com.sphere.shortvideos.helper.MoneyCacheHelper
import com.sphere.shortvideos.helper.localEvent
import com.sphere.shortvideos.helper.mmkv.MMKVRepository
import com.sphere.shortvideos.logError
import com.sphere.shortvideos.nextView
import com.sphere.shortvideos.showToast
import com.sphere.shortvideos.toJson
import com.sphere.shortvideos.view.AnimViewHelper
import com.sphere.shortvideos.view.SpineHelper
import com.sphere.shortvideos.view.initView
import com.sphere.shortvideos.view.refreshViewTagMoney
import com.sphere.shortvideos.vm.MainViewModel
import com.ss.ttvideoengine.TTVideoEngineInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PangleVideoContainerFragment : GenericFragment<FragmentPangleVideoContainerBinding>() {
    private val spineHelper = SpineHelper()

    private val viewModel by activityViewModels<MainViewModel>()

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

    override fun bindView(inflater: LayoutInflater, container: ViewGroup?) = run {
        FragmentPangleVideoContainerBinding.inflate(inflater, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun initUI() {
        initSeekbarAnim()
        spineHelper.addViewMoney1(binding.layoutAnim, requireContext())
        lifecycleScope.launch(Dispatchers.Main) {
            shortPlay?.let { item ->
                withContext(Dispatchers.IO) {
                    shortPlayCollect = database.collectDao().getItemById(item.id.toString())
                }
                var isCollected = shortPlayCollect != null
                binding.imageCollect.imageTintList = ColorStateList.valueOf(if (isCollected) ContextCompat.getColor(
                    requireContext(),
                    R.color.color_red) else Color.WHITE)
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
                    localEvent("foru_enter")
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
                                ColorStateList.valueOf(if (isCollected) ContextCompat.getColor(requireContext(),
                                    R.color.color_red) else Color.WHITE)
                        }
                    }
                }
                showDramaFragment(item)
            }
        }
        activity?.let {
            binding.layoutMoney.initView(it as MainActivity, "for_u")
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
        binding.seekbar.thumb = ContextCompat.getDrawable(requireContext(),
            if (isLarge) R.drawable.seekbar_thum_large else R.drawable.seekbar_thum)
        binding.seekbar.progressDrawable = ContextCompat.getDrawable(requireContext(),
            if (isLarge) R.drawable.seekbar_bg_large else R.drawable.seekbar_bg)
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
        detailFragment =
            PSSDK.createDetailFragment(shortPlay, builder.build(), object : PSSDK.ShortPlayDetailPageListener {
                override fun onOverScroll(index: Int) = Unit
                override fun onProgressChange(shortPlay: ShortPlay?, index: Int, currentPlayTime: Int, duration: Int) {
                    onProgressChanged.invoke(currentPlayTime, duration)
                }

                override fun onPlayFailed(e: PSSDK.ErrorInfo?): Boolean {
                    return false
                }

                override fun onShortPlayPlayed(p0: ShortPlay?, p1: Int, p2: EpisodeData?) {}


                @SuppressLint("SetTextI18n")
                override fun onItemSelected(position: Int,
                                            type: PSSDK.ShortPlayDetailPageListener.ItemType?,
                                            index: Int) = Unit

                override fun onVideoPlayStateChanged(shortPlay: ShortPlay?, index: Int, playbackState: Int) {
                    when (playbackState) {
                        PSSDK.PLAYBACK_STATE_PLAY -> {
                            videoStart()
                            MoneyCacheHelper.startWatchVideo()
                            viewModel.playMoneyProgress()
                        }

                        PSSDK.PLAYBACK_STATE_PAUSE -> {
                            MoneyCacheHelper.stopWatchVideo()
                            viewModel.pauseMoneyProgress()
                        }
                    }
                }

                override fun onVideoPlayCompleted(shortPlay: ShortPlay?, index: Int) {
                    MoneyCacheHelper.stopWatchVideo()
                    viewModel.pauseMoneyProgress()
                    requireContext().nextView<PangleDramaPlayActivity> {
                        putExtra(GlobalConstants.EXTRA_KEY_SHORT_PLAY, shortPlay)
                        putExtra(GlobalConstants.EXTRA_KEY_START_INDEX, 2)
                        putExtra(GlobalConstants.EXTRA_KEY_START_PROGRESS, 0)
                    }
                }

                override fun onEnterImmersiveMode() = Unit
                override fun onExitImmersiveMode() = Unit

                override fun isNeedBlock(shortPlay: ShortPlay?, index: Int): Boolean = false

                override fun showAdIfNeed(shortPlay: ShortPlay?, p1: Int, p2: PSSDK.ShortPlayBlockResultListener?) =
                    Unit

                override fun onVideoInfoFetched(shortPlay: ShortPlay?,
                                                index: Int,
                                                videoPlayInfo: PSSDK.VideoPlayInfo?) = Unit

                override fun onObtainPlayerControlViews(): List<View?>? = null

            })
        if (detailFragment == null) {
            requireContext().showToast(R.string.play_failed_please_try_again)
            return
        }
        // 这里有可能在 onSaveInstanceState 之后被调用，直接 commit() 会抛 IllegalStateException
        // 使用 commitAllowingStateLoss 防止因状态已保存导致的崩溃
        if (!isAdded || isDetached || view == null) {
            // 宿主 Fragment 已不再处于有效状态，直接放弃添加子 Fragment，避免状态丢失相关异常
            return
        }
        childFragmentManager.beginTransaction()
            .add(R.id.fragment_container, detailFragment!!)
            .show(detailFragment!!)
            .commitAllowingStateLoss()
    }

    fun pausePlay() {
        detailFragment?.pausePlay()
    }

    fun resumePlay() {
        (activity as? MainActivity)?.let {
            if (it.dialogFragmentNum > 0) {
                return
            }
        }
        if (isResume) {
            detailFragment?.startPlay()
        }
    }

    override fun onStop() {
        super.onStop()
        pausePlay()
    }

    private var isGo = false
    private fun videoStart() {
        if (isGo) return
        isGo = true
        showNewUser()
    }

    override fun onPause() {
        super.onPause()
        MoneyCacheHelper.stopWatchVideo()
        viewModel.pauseMoneyProgress()
    }

    override fun onResume() {
        super.onResume()
        registerMainViewModel()
        resumePlay()
    }

    override fun onDestroyView() {
        stopFingerAnim()
        super.onDestroyView()
    }

    private val numProgressObserver = Observer<Int> { value ->
        binding.progressPrice.progress = value
    }
    private val numTimeObserver = Observer<String> { value ->
        binding.tvTipsNum.text = value
    }
    private val nextRewardObserver = Observer<Int> { value ->
        when (value) {
            1 -> spineHelper.addViewMoney2(binding.layoutAnim, requireContext())
            else -> spineHelper.addViewMoney1(binding.layoutAnim, requireContext())
        }
    }

    private var isObserversRegistered = false

    private fun registerMainViewModel() {
        if (isObserversRegistered) return
        isObserversRegistered = true
        viewModel.numProgress.observe(viewLifecycleOwner, numProgressObserver)
        viewModel.numTime.observe(viewLifecycleOwner, numTimeObserver)
        viewModel.nextRewordType.observe(viewLifecycleOwner, nextRewardObserver)
        HelperRewardShow.curGetMoneyAnimLiveData.observe(this, {
            binding.layoutMoney.tvCurMoney.text = it
        })
        HelperRewardShow.curMoneyNeedAnimLiveData.observe(this, {
            binding.layoutMoney.refreshViewTagMoney(it)
        })
        HelperRewardShow.animAddMoneyDurationInMill.observe(this, {
            if (it > 0) {
                AnimViewHelper.playCoinFlyWithHitAnim(binding.ivIconAnim,
                    binding.layoutMoney.ivPack,
                    durationMs = it,
                    scaleTo = 1.4f)
                HelperRewardShow.animAddMoneyDurationInMill.value = 0
            }
        })

        showBubbleTips.observe(this, { pair ->
            logError("showBubbleTips-->$pair")
            if (System.currentTimeMillis() - pair.second < 2000) return@observe
            binding.tvTis.setTextAndDismiss(pair)
        })
    }

    private var fingerAnimator: ObjectAnimator? = null
    private fun hide() {
        (activity as? MainActivity)?.run {
            WelcomeBonusDialogFragment().apply {
                onDismissCall = {
                    if (MMKVRepository.isNewUser) {
                        HelperRewardShow.showFirstTips(it)
                        HelperRewardShow.addMoneyNotExChangeFlyAnim(it)
                    }
                    MMKVRepository.isNewUser = false
                    hideOrShowGuide(false)
                }
            }.show(supportFragmentManager, "welcome")
        }
        setGuideVisibility(View.GONE)
        stopFingerAnim()
    }

    private fun showNewUser(): Boolean {
        if (MMKVRepository.isNewUser) {
            detailFragment?.pausePlay()
            setGuideVisibility(View.VISIBLE)
            (activity as? MainActivity)?.hideOrShowGuide()
            localEvent("new_guide")
            startFingerAnim()
            binding.ivFirstGuide.setOnClickListener {
                localEvent("new_guide_c", hashMapOf("type" to "mask_2"))
                hide()
            }
            binding.ivFingerAnim.setOnClickListener {
                hide()
                localEvent("new_guide_c", hashMapOf("type" to "mask_1"))
            }
            return true
        }
        return false
    }

    private fun setGuideVisibility(sta: Int) {
        binding.ivFirstGuide.visibility = sta
        binding.ivFingerAnim.visibility = sta
        binding.tvTipsUser.visibility = sta
    }

    private fun startFingerAnim() {
        if (fingerAnimator != null) return
        binding.ivFingerAnim.scaleX = 0.8f
        binding.ivFingerAnim.scaleY = 0.8f
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.8f, 1.3f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.8f, 1.3f)
        fingerAnimator = ObjectAnimator.ofPropertyValuesHolder(binding.ivFingerAnim, scaleX, scaleY).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
        }
    }


    private fun stopFingerAnim() {
        fingerAnimator?.cancel()
        fingerAnimator = null
    }


}