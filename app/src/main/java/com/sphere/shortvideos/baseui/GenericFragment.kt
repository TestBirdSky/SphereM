package com.sphere.shortvideos.baseui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.chartboost.sdk.impl.fa
import com.sphere.shortvideos.logError

abstract class GenericFragment<VB : ViewBinding> : Fragment() {

    lateinit var binding: VB
    protected var isResume = false


    abstract fun bindView(inflater: LayoutInflater, container: ViewGroup?): VB

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return bindView(inflater, container).also { binding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
    }

    abstract fun initUI()

    override fun onPause() {
        super.onPause()
        logError("onPause-->$this")
        isResume = false
    }

    override fun onResume() {
        super.onResume()
        logError("onResume-->$this")
        isResume = true
    }

}