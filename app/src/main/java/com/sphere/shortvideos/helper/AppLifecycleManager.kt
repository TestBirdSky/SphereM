package com.sphere.shortvideos.helper

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import com.adjust.sdk.Adjust
import com.sphere.shortvideos.activity.LoadingActivity
import com.sphere.shortvideos.baseui.GenericActivity
import com.sphere.shortvideos.isInteractive
import com.sphere.shortvideos.logError
import com.sphere.shortvideos.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object AppLifecycleManager : Application.ActivityLifecycleCallbacks {
    private val activityList = mutableListOf<Activity>()
    private var restart = false
    private var backgroundJob: Job? = null
    private var foregroundCounts = 0

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        synchronized(activityList) { activityList.add(activity) }
        logError("onActivityCreated-->$activity")
    }

    override fun onActivityStarted(activity: Activity) {
        logError("onActivityStarted-->$activity")
        foregroundCounts++
        NotificationHelper.isInApp = true
        backgroundJob?.cancel()
        if (!restart) return
        restart = false
        if (isInteractive().not()) return
        activity.startActivity(Intent(activity, LoadingActivity::class.java))
    }

    override fun onActivityResumed(activity: Activity) {
        Adjust.onResume()
    }

    override fun onActivityPaused(activity: Activity) {
        Adjust.onPause()
    }

    override fun onActivityStopped(activity: Activity) {
        logError("onActivityStopped-->$activity")
        runCatching {
            backgroundJob?.cancel()
            if (--foregroundCounts <= 0) {
                NotificationHelper.isInApp = false
                backgroundJob = CoroutineScope(Dispatchers.IO).launch {
                    delay(3000L)
                    runCatching {
                        activityList.toMutableList().forEach { act ->
                            if (act !is GenericActivity) {
                                act.finish()
                            } else if (act is LoadingActivity) {
                                act.finish()
                            }
                        }
                    }
                    NotificationHelper.onBackShowNotif()
                    restart = true
                }
            }
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) {
        logError("onActivityDestroyed-->$activity")
        synchronized(activityList) { activityList.remove(activity) }
    }


}