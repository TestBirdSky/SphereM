package com.sphere.shortvideos.baseui

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewbinding.ViewBinding

abstract class GenericBindActivity<VB : ViewBinding> : GenericActivity() {
    protected var topMar = -1
    abstract val binding: VB

    override fun bindView() {
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, if (topMar!=-1) topMar else systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initUI()
    }

    abstract fun initUI()


}