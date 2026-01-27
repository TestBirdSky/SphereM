package com.sphere.shortvideos.helper

import android.app.Activity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.sphere.shortvideos.baseui.GenericActivity
import com.sphere.shortvideos.dialogs.NormalCongratulateDialogFragment
import com.sphere.shortvideos.helper.reward.RewardHelper
import com.sphere.shortvideos.logError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object HelperRewardShow {
    private var num = 1
    private var maxNum = 3
    val numTime = MutableLiveData("1/3")
    val numProgress = MutableLiveData<Int>(0)
    val nextRewordType = MutableLiveData(-1) //0 普通奖励看插屏 1 倍率玩法看激励广告
    val curGetMoneyStr = MutableLiveData(Pair("", ""))//当前获取到的奖励和还差多少可领取奖励
    val showDialogType = MutableLiveData<Int>(-1) //0 普通奖励看插屏 1 倍率玩法看激励广告
    private var maxReachedCount = 0
    private var progressJob: Job? = null
    private val progressMax = 100
    private val roundDurationMs = 5000L //15_000L todo

    private var numIntervalIndex = 0 // 目标
    private var tagGam = -1

    fun init() {
        if (curGetMoneyStr.value == Pair("", "")) {
            curGetMoneyStr.value = WithdrawAmountHelper.fetchCurMoneyAndWithdrawNeedMoney()
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
                    num += 1
                    if (num > maxNum) {
                        num = 1
                        maxReachedCount++
                        if (maxReachedCount >= tagGam) {
                            maxReachedCount = 0
                            showDialogType.postValue(1)
                            setGameTargetNum()
                            nextRewordType.postValue(if (maxReachedCount >= tagGam - 1) 1 else 0)
                        } else {
                            showDialogType.postValue(0)
                        }
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

    fun addMoney(d: Double) {
        MoneyCacheHelper.addMoney(d)
        curGetMoneyStr.postValue(WithdrawAmountHelper.fetchCurMoneyAndWithdrawNeedMoney())
    }

    fun addMoneyNotExChange(d: Double) {
        MoneyCacheHelper.addNotExchangeMoney(d)
        curGetMoneyStr.postValue(WithdrawAmountHelper.fetchCurMoneyAndWithdrawNeedMoney())
    }

    private fun setGameTargetNum() {
        val list = RewardHelper.getConfigByLanguage().adInterval
        if (numIntervalIndex >= list.size) {
            numIntervalIndex = 0
        }
        tagGam = list[numIntervalIndex]
        numIntervalIndex++
    }

    fun registerConDialog(activity: GenericActivity) {
        logError("registerConDialog-->$activity")
        showDialogType.observe(activity) {
            logError("registerConDialog-->$it")
            when (it) {
                0 -> {
                    NormalCongratulateDialogFragment().apply {
                        onClaim = {

                        }
                        onClose = {

                        }
                    }.show(activity.supportFragmentManager, "congratulation")
                }
                1 -> {

                }
            }
        }
    }
}