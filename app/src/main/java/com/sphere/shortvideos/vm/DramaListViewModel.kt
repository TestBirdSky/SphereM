package com.sphere.shortvideos.vm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bytedance.sdk.shortplay.api.PSSDK
import com.bytedance.sdk.shortplay.api.ShortPlay
import com.sphere.shortvideos.logError

class DramaListViewModel : ViewModel() {


    private var pageIndex = 1
    private var pageCount = 15
    var hasMoreData = true
    val refreshLiveData = MutableLiveData<MutableList<ShortPlay>>()
    val addLiveData = MutableLiveData<MutableList<ShortPlay>>()
    val onErrorLiveData = MutableLiveData<Boolean>()

    //获取指定短剧ID
//    fun loadIdDrama() {
////        DramaPangleHelper.getDramaById(shortPlayIds = DramaPangleHelper.testDrama(), count = 5)
//        logError("loadIdDrama--->")
//        PSSDK.requestFeedListByCategoryIds(null,
//            DramaPangleHelper.testDrama(),
//            1,
//            5,
//            object : PSSDK.FeedListResultListener {
//                override fun onFail(errorInfo: PSSDK.ErrorInfo?) {
//                    logError("loadIdDrama-->${errorInfo?.code},${errorInfo?.msg}")
//                    if (10013 == errorInfo?.code) {
//                        refreshLiveData.postValue(mutableListOf())
//                    } else onErrorLiveData.postValue(true)
//
//                }
//                override fun onSuccess(result: PSSDK.FeedListLoadResult<ShortPlay?>?) {
//                    logError("loadIdDrama-->$result")
//                    hasMoreData = result?.hasMore ?: false
//                    refreshLiveData.postValue((result?.dataList ?: mutableListOf()) as MutableList<ShortPlay>?)
//                }
//            })
//    }

    fun loadData(categoryId: Long) {
        pageIndex = 1
        val resultListener = object : PSSDK.FeedListResultListener {
            override fun onFail(errorInfo: PSSDK.ErrorInfo?) {
                logError("${errorInfo?.code},${errorInfo?.msg}")
                if (10013 == errorInfo?.code) {
                    refreshLiveData.postValue(mutableListOf())
                } else onErrorLiveData.postValue(true)
            }

            override fun onSuccess(result: PSSDK.FeedListLoadResult<ShortPlay>?) {
                hasMoreData = result?.hasMore ?: false
                refreshLiveData.postValue(result?.dataList ?: mutableListOf())
            }
        }
        when (categoryId) {
            -2L -> PSSDK.requestFeedList(pageIndex, pageCount, resultListener)
            -1L -> PSSDK.requestNewDrama(pageIndex, pageCount, resultListener)
            else -> PSSDK.requestFeedListByCategoryIds(mutableListOf(categoryId),
                null,
                pageIndex,
                pageCount,
                resultListener)
        }
    }

    fun loadMore(categoryId: Long) {
        val resultListener = object : PSSDK.FeedListResultListener {
            override fun onFail(errorInfo: PSSDK.ErrorInfo?) {
                onErrorLiveData.postValue(false)
            }

            override fun onSuccess(result: PSSDK.FeedListLoadResult<ShortPlay>?) {
                hasMoreData = result?.hasMore ?: false
                pageIndex++
                addLiveData.postValue(result?.dataList ?: mutableListOf())
            }
        }
        when (categoryId) {
            -2L -> PSSDK.requestFeedList(pageIndex + 1, pageCount, resultListener)
            -1L -> PSSDK.requestNewDrama(pageIndex + 1, pageCount, resultListener)
            else -> PSSDK.requestFeedListByCategoryIds(mutableListOf(categoryId),
                null,
                pageIndex + 1,
                pageCount,
                resultListener)
        }
    }

}