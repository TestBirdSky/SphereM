package com.sphere.shortvideos.activity

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bytedance.sdk.shortplay.api.EpisodeData
import com.bytedance.sdk.shortplay.api.PSSDK
import com.bytedance.sdk.shortplay.api.ShortPlay
import com.bytedance.sdk.shortplay.api.ShortPlayFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.sphere.shortvideos.GlobalConstants
import com.sphere.shortvideos.R
import com.sphere.shortvideos.baseui.GenericBindActivity
import com.sphere.shortvideos.database
import com.sphere.shortvideos.database.DramaCollectEntity
import com.sphere.shortvideos.database.DramaEpisodeEntity
import com.sphere.shortvideos.database.DramaHistoryEntity
import com.sphere.shortvideos.databinding.ActivityDramaPlayPangleBinding
import com.sphere.shortvideos.dialogs.TaskInfoDialogFragment
import com.sphere.shortvideos.dialogs.showIndexSelectorDialog
import com.sphere.shortvideos.helper.HelperRewardShow
import com.sphere.shortvideos.helper.MoneyCacheHelper
import com.sphere.shortvideos.helper.ad.AdUtils
import com.sphere.shortvideos.helper.localEvent
import com.sphere.shortvideos.showToast
import com.sphere.shortvideos.toJson
import com.sphere.shortvideos.unlockIndex
import com.sphere.shortvideos.view.SpineHelper
import com.ss.ttvideoengine.Resolution
import com.ss.ttvideoengine.TTVideoEngineInterface
import com.ss.ttvideoengine.model.VideoRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION")
class PangleDramaPlayActivity : GenericBindActivity<ActivityDramaPlayPangleBinding>() {

    private val shortPlay by lazy { intent?.getParcelableExtra<ShortPlay>(GlobalConstants.EXTRA_KEY_SHORT_PLAY) }
    private val startIndex by lazy { intent?.getIntExtra(GlobalConstants.EXTRA_KEY_START_INDEX, 1) ?: 1 }
    private val startProgress by lazy { intent?.getIntExtra(GlobalConstants.EXTRA_KEY_START_PROGRESS, 0) ?: 0 }
    private val needShowPlaylist by lazy {
        intent?.getBooleanExtra(GlobalConstants.EXTRA_KEY_COMMON_BOOLEAN, false) ?: false
    }
    private var shortPlayHistory: DramaHistoryEntity? = null
    private var shortPlayCollect: DramaCollectEntity? = null
    private var episodeEntity: DramaEpisodeEntity? = null
    private var currentIndex = 1
    private var currentProgress = 0
    private var detailFragment: ShortPlayFragment? = null
    private var resolutions: Array<Resolution>? = null
    private var currentResolution: Resolution? = null
    private var onProgressChanged: (progress: Int, max: Int) -> Unit = { _, _ -> }
    private var onVideoInfoFetched: (resolutions: Array<Resolution>?, currentResolution: Resolution?) -> Unit =
        { _, _ -> }
    private var controlViewHideJob: Job? = null
    private val speedArr = arrayOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    private var isForceShowAd: Boolean = false
    private var lastIsEven: Boolean = false

    override val binding by lazy { ActivityDramaPlayPangleBinding.inflate(layoutInflater) }

    @SuppressLint("SetTextI18n")
    override fun initUI() {
        initSeekbarAnim()
        bindClick()
        lifecycleScope.launch(Dispatchers.Main) {
            AdUtils.unlockHolder.preloadIfCan()
            shortPlay?.let { item ->
                withContext(Dispatchers.IO) {
                    shortPlayHistory =
                        intent?.getParcelableExtra(GlobalConstants.EXTRA_KEY_DRAMA_HISTORY) ?: database.historyDao()
                            .getItemById(item.id.toString())
                    if (null != shortPlayHistory) {
                        val historyIndex = shortPlayHistory?.currentIndex ?: 1
                        currentIndex = maxOf(startIndex, historyIndex)
                        currentProgress =
                            if (startIndex > historyIndex) startProgress else shortPlayHistory?.currentProgress ?: 0
                    } else {
                        shortPlayHistory = DramaHistoryEntity(Gson().toJson(item),
                            item.id.toString(),
                            startIndex,
                            startProgress,
                            100,
                            System.currentTimeMillis())
                        val uid = database.historyDao().upsert(shortPlayHistory!!)
                        shortPlayHistory?.uid = uid
                        currentIndex = shortPlayHistory?.currentIndex ?: 1
                        currentProgress = shortPlayHistory?.currentProgress ?: 0
                    }
                    shortPlayCollect = database.collectDao().getItemById(item.id.toString())
                    episodeEntity = database.episodeDao().getItemById(item.id.toString())
                    if (null == episodeEntity) {
                        episodeEntity = DramaEpisodeEntity(dramaId = item.id.toString())
                        val uid = database.episodeDao().upsert(episodeEntity!!)
                        episodeEntity?.uid = uid
                    }
                }
                var isCollected = shortPlayCollect != null
                binding.imageCollect.imageTintList = ColorStateList.valueOf(if (isCollected) ContextCompat.getColor(
                    activity,
                    R.color.color_red) else Color.WHITE)
                binding.seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) = Unit
                    override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        detailFragment?.setCurrentPlayTimeSeconds(seekBar?.progress ?: 0)
                        controlViewHideJob = lifecycleScope.launch(Dispatchers.Main) {
                            delay(5000L)
                            binding.groupControl.visibility = View.INVISIBLE
                        }
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
                binding.textPlayEp.text = "Ep.${currentIndex} | Eps.${item.total}"
                binding.btnEp.setOnClickListener {
                    episodeEntity?.let { data ->
                        detailFragment?.pausePlay()
                        showIndexSelectorDialog(item, currentIndex, data, onChoose = { index ->
                            detailFragment?.startPlayIndex(index)
                        }, onCancel = {
                            detailFragment?.startPlay()
                        })
                    }
                }
                binding.layoutPlayList.setOnClickListener {
                    episodeEntity?.let { data ->
                        detailFragment?.pausePlay()
                        showIndexSelectorDialog(item, currentIndex, data, onChoose = { index ->
                            detailFragment?.startPlayIndex(index)
                        }, onCancel = {
                            detailFragment?.startPlay()
                        })
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
                                ColorStateList.valueOf(if (isCollected) ContextCompat.getColor(activity,
                                    R.color.color_red) else Color.WHITE)
                        }
                    }
                }
                binding.btnSpeed.setOnClickListener {
                    MaterialAlertDialogBuilder(activity).setTitle(getString(R.string.speed))
                        .setItems(speedArr.map { "$it" }.toTypedArray()) { dialog, which ->
                            val curSpeed = speedArr.getOrNull(which) ?: 1.0f
                            binding.btnSpeed.text = "${curSpeed}x"
                            detailFragment?.setVideoSpeed(curSpeed)
                        }.show()
                }
                binding.btnResolution.setOnClickListener {
                    if (resolutions.isNullOrEmpty()) return@setOnClickListener
                    MaterialAlertDialogBuilder(activity).setTitle(getString(R.string.resolution))
                        .setItems((resolutions ?: arrayOf()).map { it.toString(VideoRef.TYPE_VIDEO) }
                            .toTypedArray()) { dialog, which ->
                            val curResolution = resolutions?.getOrNull(which) ?: Resolution.SuperHigh
                            binding.btnResolution.text = "${curResolution.toString(VideoRef.TYPE_VIDEO)}"
                            detailFragment?.setResolution(curResolution)
                        }.show()
                }
                onVideoInfoFetched = { resolutions, currentResolution ->
                    binding.btnResolution.text = "${currentResolution?.toString(VideoRef.TYPE_VIDEO)}"
                }
                showDramaFragment(item)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initSeekbarAnim() {
        binding.seekbar.setOnTouchListener { view, event ->
            controlViewHideJob?.cancel()
            when (event.action) {
                MotionEvent.ACTION_DOWN -> changeSeekbarSize(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> changeSeekbarSize(false)
            }
            false
        }
    }

    private fun changeSeekbarSize(isLarge: Boolean) {
        binding.seekbar.thumb =
            ContextCompat.getDrawable(activity, if (isLarge) R.drawable.seekbar_thum_large else R.drawable.seekbar_thum)
        binding.seekbar.progressDrawable =
            ContextCompat.getDrawable(activity, if (isLarge) R.drawable.seekbar_bg_large else R.drawable.seekbar_bg)
    }

    private fun showDramaFragment(shortPlay: ShortPlay) {
        val builder = PSSDK.DetailPageConfig.Builder()
        builder.hideLeftTopCloseAndTitle(false, PSSDK.ShortPlayDetailPageCloseListener {
            onBackActioned()
            return@ShortPlayDetailPageCloseListener true
        }).apply {
            displayBottomExtraView(false)
            startPlayIndex(currentIndex)
            startPlayAtTimeSeconds(currentProgress)
            enableAutoPlayNext(true)
            displayProgressBar(false)
            setVideoDisplayMode(TTVideoEngineInterface.IMAGE_LAYOUT_ASPECT_FILL)
            displayTextVisibility(PSSDK.DetailPageConfig.TEXT_POS_BOTTOM_TITLE, false)
            displayTextVisibility(PSSDK.DetailPageConfig.TEXT_POS_BOTTOM_DESC, false)
        }
        detailFragment =
            PSSDK.createDetailFragment(shortPlay, builder.build(), object : PSSDK.ShortPlayDetailPageListener {
                override fun onOverScroll(index: Int) = Unit
                override fun onProgressChange(shortPlay: ShortPlay?, index: Int, currentPlayTime: Int, duration: Int) {
                    onProgressChanged.invoke(currentPlayTime, duration)
                    shortPlay?.let { item ->
                        shortPlayHistory?.run {
                            dataJson = Gson().toJson(item)
                            currentIndex = index
                            currentProgress = currentPlayTime
                            maxProgress = duration
                            lastWatchTime = System.currentTimeMillis()
                        }
                    }
                }

                override fun onPlayFailed(e: PSSDK.ErrorInfo?): Boolean {
                    return false
                }

                override fun onShortPlayPlayed(p0: ShortPlay?, p1: Int, p2: EpisodeData?) {}

                @SuppressLint("SetTextI18n")
                override fun onItemSelected(position: Int,
                                            type: PSSDK.ShortPlayDetailPageListener.ItemType?,
                                            index: Int) {
                    controlViewHideJob?.cancel()
                    resolutions = null
                    currentResolution = null
                    onProgressChanged.invoke(0, 0)
                    currentIndex = index
                    lifecycleScope.launch(Dispatchers.Main) {
                        binding.textPlayEp.text = "Ep.${currentIndex} | Eps.${shortPlay.total}"
                    }
                    val lastEven = lastIsEven
                    val currentIsEven = index % 2 == 0
                    lastIsEven = currentIsEven
                    if (index >= unlockIndex) localEvent("ds_ad_chance", hashMapOf("ad_pos_id" to "ds_unlock_int"))
                    if (AdUtils.unlockHolder.isAdHaveCache()) {
                        if (isOddGreaterThanRemote(index)) {
                            showUnlockAd(index)
                        } else if (isForceShowAd) {
                            isForceShowAd = false
                            showUnlockAd(index)
                        } else {
                            if (lastEven && currentIsEven) isForceShowAd = true
                            episodeEntity?.let { updateEpisodeData(shortPlay, it, index) }
                        }
                    } else {
                        episodeEntity?.let { updateEpisodeData(shortPlay, it, index) }
                        AdUtils.unlockHolder.preloadIfCan()
                        isForceShowAd = true
                    }
                }

                fun isOddGreaterThanRemote(n: Int): Boolean {
                    return n >= unlockIndex && n % 2 == 1
                }

                fun showUnlockAd(index: Int) {
                    AdUtils.unlockHolder.showFullAd(activity, eventName = "ds_unlock_int", onAdDismissed = {
                        episodeEntity?.let {
                            updateEpisodeData(shortPlay, it, index)
                        }
                    })
                }

                fun updateEpisodeData(shortPlay: ShortPlay, episodeEntity: DramaEpisodeEntity, chooseIndex: Int) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val newArr = episodeEntity.numbers.toMutableSet().apply { add(chooseIndex) }.toMutableList()
                        episodeEntity.numbers = newArr
                        database.episodeDao().updateItemById(shortPlay.id.toString(), newArr)
                    }
                }

                override fun onVideoPlayStateChanged(shortPlay: ShortPlay?, index: Int, playbackState: Int) {
                    if (PSSDK.PLAYBACK_STATE_PAUSE == playbackState && shortPlayHistory != null) {
                        shortPlayHistory?.let {
                            lifecycleScope.launch(Dispatchers.IO) {
                                database.historyDao().upsert(it)
                            }
                        }
                    }
                    when (playbackState) {
                        PSSDK.PLAYBACK_STATE_PAUSE -> {
                            HelperRewardShow.pauseMoneyProgress()
                            MoneyCacheHelper.stopWatchVideo()
                            controlViewHideJob?.cancel()
                            binding.groupControl.visibility = View.VISIBLE
                            changeSeekbarSize(true)
                        }

                        PSSDK.PLAYBACK_STATE_PLAY -> {
                            HelperRewardShow.playMoneyProgress()
                            MoneyCacheHelper.startWatchVideo()
                            changeSeekbarSize(false)
                            controlViewHideJob = lifecycleScope.launch(Dispatchers.Main) {
                                delay(5000L)
                                binding.groupControl.visibility = View.INVISIBLE
                            }
                        }
                    }
                }

                override fun onVideoPlayCompleted(shortPlay: ShortPlay?, index: Int) {
                    MoneyCacheHelper.stopWatchVideo()
                    HelperRewardShow.pauseMoneyProgress()
                }

                override fun onEnterImmersiveMode() = Unit
                override fun onExitImmersiveMode() = Unit

                override fun isNeedBlock(shortPlay: ShortPlay?, index: Int): Boolean = false

                override fun showAdIfNeed(shortPlay: ShortPlay?, p1: Int, p2: PSSDK.ShortPlayBlockResultListener?) =
                    Unit

                override fun onVideoInfoFetched(shortPlay: ShortPlay?,
                                                index: Int,
                                                videoPlayInfo: PSSDK.VideoPlayInfo?) {
                    resolutions = videoPlayInfo?.supportResolutions
                    currentResolution = videoPlayInfo?.currentResolution
                    onVideoInfoFetched.invoke(resolutions, currentResolution)
                }

                override fun onObtainPlayerControlViews(): List<View?>? = null

            })
        if (detailFragment == null) {
            showToast(R.string.play_failed_please_try_again)
            return
        }
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, detailFragment!!).show(detailFragment!!)
            .commit()
    }

    override fun onPause() {
        super.onPause()
        shortPlayHistory?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                database.historyDao().upsert(it)
            }
        }
    }

    private fun bindClick() {
        binding.layoutTop.setOnClickListener {
            TaskInfoDialogFragment().show(supportFragmentManager,"task_fragment")
        }
        registerMainViewModel()
    }

    private val spineHelper = SpineHelper()

    private fun registerMainViewModel() {
        HelperRewardShow.numProgress.observe(this, {
            binding.progressPrice.progress = it
        })
        HelperRewardShow.numTime.observe(this, {
            binding.tvTipsNum.text = it
        })
        HelperRewardShow.nextRewordType.observe(this, {
            when (it) {
                1 -> {
                    spineHelper.addViewMoney2(binding.layoutAnim, this)
                }

                else -> {
                    spineHelper.addViewMoney1(binding.layoutAnim, this)
                }
            }
        })
        HelperRewardShow.curGetMoneyStr.observe(this, {
            binding.tvCurMoney.text = it.first
        })
        HelperRewardShow.registerConDialog(activity)
    }

}