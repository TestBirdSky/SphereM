package com.sphere.shortvideos.vm

import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sphere.shortvideos.database
import com.sphere.shortvideos.database.DramaCollectEntity
import com.sphere.shortvideos.database.DramaHistoryEntity
import com.sphere.shortvideos.helper.AddMoneyListener
import com.sphere.shortvideos.helper.HelperRewardShow
import com.sphere.shortvideos.helper.MoneyCacheHelper
import com.sphere.shortvideos.helper.WithdrawAmountHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    var fragmentNum = MutableLiveData(0)
    val shouldPauseVideoByDialog = MutableLiveData(false)
    val historyLiveData = MutableLiveData<List<DramaHistoryEntity>>()
    val collectionLiveData = MutableLiveData<List<DramaCollectEntity>>()

    fun collectHistoryRoom() {
        viewModelScope.launch(Dispatchers.IO) {
            database.historyDao().getAll().distinctUntilChanged().collect {
                historyLiveData.postValue(it)
            }
        }
    }

    fun fetchCollectionRoom() {
        viewModelScope.launch(Dispatchers.IO) {
            database.collectDao().getAll().distinctUntilChanged().collect {
                collectionLiveData.postValue(it)
            }
        }
    }

    val numTime = HelperRewardShow.numTime
    val numProgress = HelperRewardShow.numProgress
    val nextRewordType = HelperRewardShow.nextRewordType
    val curGetMoneyStr = HelperRewardShow.curGetMoneyStr

    init {
        HelperRewardShow.init()
    }

    fun playMoneyProgress() {
        HelperRewardShow.playMoneyProgress()
    }

    fun pauseMoneyProgress() {
        HelperRewardShow.pauseMoneyProgress()
    }

    fun newUserProgress() {
        HelperRewardShow.newUserProgress()
    }


    fun addMoneyNotExChange(d: Double) {
        HelperRewardShow.addMoneyNotExChange(d)
    }

    fun onPauseDialogShown() {
        val next = (fragmentNum.value ?: 0) + 1
        fragmentNum.value = next
        shouldPauseVideoByDialog.value = true
    }

    fun onPauseDialogDismissed() {
        val next = ((fragmentNum.value ?: 0) - 1).coerceAtLeast(0)
        fragmentNum.value = next
        shouldPauseVideoByDialog.value = next > 0
    }

}