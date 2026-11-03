package com.sphere.shortvideos.notification

import android.content.Context

/**
 * Date：2026/1/21
 * Describe:
 */
class NotificationImpl(val notificationId: Int = 1000,
                       val strTitle: ArrayList<Int>,
                       val strContext: ArrayList<Int>) {
    private var index = 0

    fun showNotification(context: Context) {
        if (strTitle.isEmpty() || strContext.isEmpty()) return
        val total = minOf(strTitle.size, strContext.size)
        if (total <= 0) return
        val safeIndex = ((index % total) + total) % total
        val title = context.getString(strTitle[safeIndex])
        val contextStr = context.getString(strContext[safeIndex])
        NotificationHelper.showUnifiedNotification(context, notificationId, title, contextStr)
        index = (safeIndex + 1) % total
    }
}
