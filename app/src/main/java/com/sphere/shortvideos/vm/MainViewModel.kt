package com.sphere.shortvideos.vm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sphere.shortvideos.database
import com.sphere.shortvideos.database.DramaCollectEntity
import com.sphere.shortvideos.database.DramaHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

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

    private var num = 1
    private var MAX_NUM = 3
    var numTime = MutableLiveData("1/3")
    var numProgress = MutableLiveData<Int>(0)
    var maxReachedCount = 0
    private var progressJob: Job? = null
    private val progressMax = 100
    private val roundDurationMs = 15_000L

    fun playMoneyProgress() {
        if (progressJob?.isActive == true) return
        val stepDelayMs = roundDurationMs / progressMax
        progressJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(stepDelayMs)
                var nextProgress =(numProgress.value ?: 0) + 1
                if (nextProgress >= progressMax) {
                    nextProgress = 0
                    num += 1
                    if (num > MAX_NUM) {
                        num = 1
                        maxReachedCount++
                    }
                    numTime.postValue("$num/$MAX_NUM")
                }
                numProgress.postValue(nextProgress)
            }
        }
    }

    fun pauseMoneyProgress() {
        progressJob?.cancel()
        progressJob = null
    }

}