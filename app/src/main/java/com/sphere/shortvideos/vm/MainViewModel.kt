package com.sphere.shortvideos.vm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sphere.shortvideos.database
import com.sphere.shortvideos.database.DramaCollectEntity
import com.sphere.shortvideos.database.DramaHistoryEntity
import kotlinx.coroutines.Dispatchers
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

}