package com.sphere.shortvideos.baseui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

abstract class GenericActivity : AppCompatActivity() {

    private var isActivityResumed: Boolean = false
    lateinit var activity: GenericActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = this
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        resources.displayMetrics.apply {
            density = heightPixels / 760f
            densityDpi = (density * 160f).toInt()
            scaledDensity = density
        }
        onBackPressedDispatcher.addCallback { onBackActioned() }
        bindView()
    }

    fun getActivityState() = isActivityResumed

    override fun onStart() {
        super.onStart()
        isActivityResumed = false
    }

    override fun onResume() {
        super.onResume()
        isActivityResumed = true
    }

    override fun onPause() {
        isActivityResumed = false
        super.onPause()
    }

    override fun onStop() {
        isActivityResumed = false
        super.onStop()
    }

    override fun onDestroy() {
        isActivityResumed = false
        super.onDestroy()
    }

    abstract fun bindView()

    open fun onBackActioned() = finish()

}