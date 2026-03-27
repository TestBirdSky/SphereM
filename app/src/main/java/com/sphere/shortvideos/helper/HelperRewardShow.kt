package com.sphere.shortvideos.helper

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.SystemClock
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.chartboost.sdk.impl.ac
import com.chartboost.sdk.impl.fa
import com.chartboost.sdk.impl.pa
import com.sphere.shortvideos.R
import com.sphere.shortvideos.activity.MainActivity
import com.sphere.shortvideos.baseui.GenericActivity
import com.sphere.shortvideos.dialogs.LuckChallengeDialogFragment
import com.sphere.shortvideos.dialogs.NormalCongratulateDialogFragment
import com.sphere.shortvideos.dialogs.withdraw.WithdrawReadyDialogFragment
import com.sphere.shortvideos.helper.ad.AdUtils
import com.sphere.shortvideos.helper.mmkv.MMKVRepository
import com.sphere.shortvideos.helper.reward.RewardHelper
import com.sphere.shortvideos.helper.withdraw.WithdrawalActionHelper
import com.sphere.shortvideos.isDebugMode
import com.sphere.shortvideos.logError
import com.sphere.shortvideos.mApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

object HelperRewardShow {
    private var num = 1
    private var maxNum = 3
    val numTime = MutableLiveData("1/3")
    val numProgress = MutableLiveData<Int>(1)
    val nextRewordType = MutableLiveData(-1) //0 普通奖励看插屏 1 倍率玩法看激励广告
    val curGetMoneyStr = MutableLiveData(Pair("", "")) //当前获取到的奖励和还差多少可领取奖励
    val showDialogType = MutableLiveData<Int>() //0 普通奖励看插屏 1 倍率玩法看激励广告

    private var lastShowTipsMoneyTime = System.currentTimeMillis()
    private var showTipsPeriod = if (isDebugMode) 60000 else 60000 * 3

    val showBubbleTips = MutableLiveData<Triple<Int, Long, String>>() // 显示的词条 显示时间
    private val showDialogFetchMoney = MutableLiveData<Long>() // 显示的词条 显示时间

    var isShowCanCash = true

    private var maxReachedCount = 0
    private var progressJob: Job? = null
    private val progressMax = 100
    private val roundDurationMs = /*if (isDebugMode) 5000L else */ 15000L //15_000L //

    private var numIntervalIndex = 0 // 目标
    private var tagGam = -1
    private val scopMain = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun init() {
        if (curGetMoneyStr.value == Pair("", "")) {
            val pair = WithdrawAmountHelper.fetchCurMoneyAndWithdrawNeedMoney()
            curGetMoneyStr.value = pair
            curGetMoneyAnimLiveData.value = pair.first
            curMoneyNeedAnimLiveData.value = pair.second
        }
    }

    fun playMoneyProgress() {
        if (tagGam == -1) {
            setGameTargetNum()
        }
        if (progressJob?.isActive == true) return
        val stepDelayMs = roundDurationMs / progressMax
        nextRewordType.postValue(if (maxReachedCount >= tagGam - 1) 1 else 0)
        progressJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                delay(stepDelayMs)
                var nextProgress = (numProgress.value ?: 0) + 1
                if (nextProgress >= progressMax) {
                    nextProgress = 0
                    if (num == 2) {
                        addMoneyInTwoWatchVideo(this)
                    }
                    num += 1
                    if (num > maxNum) {
                        num = 1
                        maxReachedCount++
                        if (maxReachedCount >= tagGam) {
                            maxReachedCount = 0
                            postOnceDialogType(1)
                            setGameTargetNum()
                            nextRewordType.postValue(if (maxReachedCount >= tagGam - 1) 1 else 0)
                        } else {
                            postOnceDialogType(0)
                        }
                    } else {
                        checkNextTips()
                    }
                    numTime.postValue("$num/$maxNum")
                }
                numProgress.postValue(nextProgress)
            }
        }
    }

    fun pauseMoneyProgress() {
        progressJob?.cancel()
        progressJob = null
    }

    fun newUserProgress() {
        numProgress.postValue(100)
    }

    fun addMoneyNotExChange(d: Double) {
        MoneyCacheHelper.addNotExchangeMoney(d)
        isArriveMinMoney()
        playMoneyIncreaseAnimAndSave(d, {
            curGetMoneyStr.postValue(WithdrawAmountHelper.fetchCurMoneyAndWithdrawNeedMoney())
        })
    }

    fun reduceMoney(d: Double) {
        if (d <= 0.0) return
        MoneyCacheHelper.reduceNotExchangeMoney(d)
        val pair = WithdrawAmountHelper.fetchCurMoneyAndWithdrawNeedMoney()
        curGetMoneyStr.postValue(pair)
        curGetMoneyAnimLiveData.postValue(pair.first)
        curMoneyNeedAnimLiveData.postValue(pair.second)
    }


    fun addMoneyNotExChangeFlyAnim(d: Double, animTime: Long = 800) { //  添加飞的动画在加钱
        scopMain.launch {
            val animTime = 800L
            animAddMoneyDurationInMill.postValue(animTime)
            delay(animTime)
            addMoneyNotExChange(d)
            animAddMoneyDurationInMill.postValue(0)
        }
    }

    val curGetMoneyAnimLiveData = MutableLiveData<String>()
    val curMoneyNeedAnimLiveData = MutableLiveData<String>() // 还差多少的钱体现的动画

    val animAddMoneyDurationInMill = MutableLiveData<Long>(0)

    private var moneyAnimator: ValueAnimator? = null
    private var moneyNeedAnimator: ValueAnimator? = null


    private fun addMoneyInTwoWatchVideo(scope: CoroutineScope) {
        val addReward = MoneyCacheHelper.fetchWatchVideoReward()
        addMoneyNotExChangeFlyAnim(addReward)
    }

    private fun playMoneyIncreaseAnimAndSave(reward: Double, end: () -> Unit) {
        moneyAnimator?.cancel()
        moneyNeedAnimator?.cancel()
        if (reward <= 0) return
        val durationMs = run {
            val minReward = 0.02
            val maxReward = 4.0
            val steps = 4 // 300..700 step 100 => 5 levels
            val ratio = ((reward - minReward) / (maxReward - minReward)).coerceIn(0.0, 1.0)
            val stepIndex = kotlin.math.floor(ratio * steps).toInt().coerceIn(0, steps)
            300L + stepIndex * 100L
        }
        val endValue = MoneyCacheHelper.fetchCurMoney().coerceAtLeast(0.0)
        val startValue = (endValue - reward).coerceAtLeast(0.0)
        val total = WithdrawAmountHelper.fetchCurMoneyAndGetMoneyMinValue().second
        val startNeed = (total - startValue).coerceAtLeast(0.0)
        val endNeed = (total - endValue).coerceAtLeast(0.0)
        moneyAnimator = ValueAnimator.ofFloat(startValue.toFloat(), endValue.toFloat()).apply {
            duration = durationMs
            interpolator = AccelerateDecelerateInterpolator()
            var lastUpdateTime = 0L
            addUpdateListener { animator ->
                val now = SystemClock.uptimeMillis()
                if (now - lastUpdateTime < 20L) return@addUpdateListener
                lastUpdateTime = now
                val value = (animator.animatedValue as Float).toDouble()
                curGetMoneyAnimLiveData.postValue(WithdrawAmountHelper.moneyFormatAddUnit(value))
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    curGetMoneyAnimLiveData.postValue(WithdrawAmountHelper.moneyFormatAddUnit(endValue))
                    end.invoke()
                }
            })
            start()
        }
        moneyNeedAnimator = ValueAnimator.ofFloat(startNeed.toFloat(), endNeed.toFloat()).apply {
            duration = durationMs
            interpolator = AccelerateDecelerateInterpolator()
            var lastUpdateTime = 0L
            addUpdateListener { animator ->
                val now = SystemClock.uptimeMillis()
                if (now - lastUpdateTime < 60L) return@addUpdateListener
                lastUpdateTime = now
                val value = (animator.animatedValue as Float).toDouble()
                curMoneyNeedAnimLiveData.postValue(WithdrawAmountHelper.moneyFormatAddUnitWithNoSpace(value))
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    curMoneyNeedAnimLiveData.postValue(WithdrawAmountHelper.moneyFormatAddUnitWithNoSpace(endNeed))
                }
            })
            start()
        }
    }


    private fun setGameTargetNum() {
        val list = RewardHelper.getConfigByLanguage().adInterval
        if (numIntervalIndex >= list.size) {
            numIntervalIndex = 0
        }
        tagGam = list[numIntervalIndex]
        numIntervalIndex++
    }

    private fun postOnceDialogType(int: Int) {
        showDialogType.postValue(int)
    }

    fun registerGetMoney(activity: GenericActivity) {
        showDialogFetchMoney.observe(activity) {
            if (DialogFragmentDisplayHelper.hasDialogFragmentShowing(activity).not()) {
                WithdrawReadyDialogFragment.showIfEligible(activity.supportFragmentManager)
            }
        }
    }

    fun registerConDialog(activity: GenericActivity) {
        showDialogType.observe(activity) {
            logError("registerConDialog-->$it")
            if (it == -1) return@observe
            when (it) {
                0 -> {
                    NormalCongratulateDialogFragment().apply {
                        onClaim = { reward ->
                            localEvent("ad_chance", hashMapOf("ad_pos_id" to "dlmsf_video_rv"))
                            showRvAd(activity, reward, adPositionName = "dlmsf_video_rv")
                        }
                        onClose = { reward ->
                            AdUtils.showRateAd(activity, isRate = {
                                localEvent("ad_chance", hashMapOf("ad_pos_id" to "dlmsf_video_close_int"))
                            }, adPositionName = "dlmsf_video_close_int")
                        }
                        onNormalClick = { reward ->
                            AdUtils.showRateAd(activity, adPositionName = "dlmsf_video_int", dismiss = {
                                addMoneyNotExChangeFlyAnim(reward)
                            }, isRate = {
                                localEvent("ad_chance", hashMapOf("ad_pos_id" to "dlmsf_video_int"))
                            })
                        }
                    }.show(activity.supportFragmentManager, "congratulation")
                }

                1 -> {
                    LuckChallengeDialogFragment().apply {
                        onResult = { reward ->
                            localEvent("ad_chance", hashMapOf("ad_pos_id" to "dlmsf_wheel_rv"))
                            showRvAd(activity, reward, adPositionName = "dlmsf_wheel_rv")
                        }
                    }.show(activity.supportFragmentManager, "luck")
                }
            }
            showDialogType.value = -1
        }
    }

    private fun showRvAd(activity: GenericActivity, reward: Double, adPositionName: String) {
        AdUtils.showRvAd(activity, adPositionName = adPositionName, dismiss = { isFetchReward ->
            if (isFetchReward) {
                addMoneyNotExChangeFlyAnim(reward)

                if (activity is MainActivity) {
                    activity.showNotificationOpen(false)
                }
            }
        })
    }

    fun isPauseFragment(fragment: Fragment): Boolean {
        return fragment is DialogFragment
    }

    fun showFirstTips(dub: Double) {
        if (WithdrawalActionHelper.getConfig().isOpenWithdraw().not()) return
        lastShowTipsMoneyTime = System.currentTimeMillis()
        showBubbleTips.postValue(Triple(0,
            System.currentTimeMillis(),
            mApp.getString(R.string.add_money_info_tips1, WithdrawAmountHelper.moneyFormatAddUnitWithNoSpace(dub))))
    }

    fun isArriveMinMoney() {
        if (WithdrawalActionHelper.getConfig().isOpenWithdraw().not()) return
        showArriveMoney()
        if (isShowCanCash.not()) return
        if (WithdrawAmountHelper.isCanWithdraw()) {
            lastShowTipsMoneyTime = System.currentTimeMillis()
            showBubbleTips.postValue(Triple(2, System.currentTimeMillis(), ""))
        }
    }

    fun showArriveMoney() {
        if (MMKVRepository.hasShownWithdrawReadyDialogEver) return
        if (WithdrawAmountHelper.isCanWithdraw()) {
            showDialogFetchMoney.postValue(System.currentTimeMillis())
        }
    }

    fun checkNextTips() {
        logError("checkNextTips-->${WithdrawalActionHelper.getConfig()}")
        if (WithdrawalActionHelper.getConfig().isOpenWithdraw().not()) return
        if (WithdrawAmountHelper.isCanWithdraw()) return
        if (System.currentTimeMillis() - lastShowTipsMoneyTime > showTipsPeriod) {
            lastShowTipsMoneyTime = System.currentTimeMillis()
            showBubbleTips.postValue(Triple(1, System.currentTimeMillis(), curMoneyNeedAnimLiveData.value ?: ""))
        }
    }
}