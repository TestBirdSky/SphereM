package com.sphere.shortvideos.vm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bytedance.sdk.shortplay.api.PSSDK
import com.bytedance.sdk.shortplay.api.PSSDK.ErrorInfo
import com.bytedance.sdk.shortplay.api.PSSDK.FeedListLoadResult
import com.bytedance.sdk.shortplay.api.PSSDK.FeedListResultListener
import com.bytedance.sdk.shortplay.api.ShortPlay
import com.sphere.shortvideos.logError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class StreamViewModel : ViewModel() {

    val refreshLiveData = MutableLiveData<MutableList<ShortPlay>>()
    val onErrorLiveData = MutableLiveData<Boolean>()

    fun loadData() {
        logError("loadData")
        viewModelScope.launch(Dispatchers.IO) {
            val result = requestFeedListSuspend()
            if (null == result || result.dataList.isNullOrEmpty()) {
                onErrorLiveData.postValue(true)
            } else {
                refreshLiveData.postValue(result.dataList)
            }
        }
    }

    private suspend fun requestFeedListSuspend(): FeedListLoadResult<ShortPlay>? = suspendCancellableCoroutine { continuation ->
        val resultListener = object : FeedListResultListener {
            override fun onFail(errorInfo: ErrorInfo?) {
                continuation.resume(null)
            }

            override fun onSuccess(result: FeedListLoadResult<ShortPlay>?) {
                continuation.resume(result)
            }
        }
        PSSDK.requestFeedList(1, 100, resultListener)
    }

}