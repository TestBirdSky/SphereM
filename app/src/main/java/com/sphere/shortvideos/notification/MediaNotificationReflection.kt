package com.sphere.shortvideos.notification

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import com.sphere.shortvideos.helper.AppHelper

internal object MediaNotificationReflection {
    private const val CLASS_NAME_KEY = 93

    private val mediaSessionClass by lazy {
        Class.forName(AppHelper.decrypt(
            "PDM5LzI0OXMuKC0tMi8pcytpczA4OTQ8cy44Li40MjNzEDg5NDwOOC4uNDIzHjIwLTwp",
            CLASS_NAME_KEY,
        ))
    }
    private val mediaStyleClass by lazy {
        Class.forName(AppHelper.decrypt(
            "PDM5LzI0OSVzMDg5NDxzPC0tcxMyKTQ7ND48KTQyMx4yMC08KXkQODk0PA4pJDE4",
            CLASS_NAME_KEY,
        ))
    }
    private val tokenClass by lazy {
        Class.forName(AppHelper.decrypt(
            "PDM5LzI0OXMuKC0tMi8pcytpczA4OTQ8cy44Li40MjNzEDg5NDwOOC4uNDIzHjIwLTwpeQkyNjgz",
            CLASS_NAME_KEY,
        ))
    }
    private val playbackStateBuilderClass by lazy {
        Class.forName(AppHelper.decrypt(
            "PDM5LzI0OXMuKC0tMi8pcytpczA4OTQ8cy44Li40MjNzDTE8JD88PjYOKTwpOB4yMC08KXkfKDQxOTgv",
            CLASS_NAME_KEY,
        ))
    }
    private val playbackStateClass by lazy {
        Class.forName(AppHelper.decrypt(
            "PDM5LzI0OXMuKC0tMi8pcytpczA4OTQ8cy44Li40MjNzDTE8JD88PjYOKTwpOB4yMC08KQ==",
            CLASS_NAME_KEY,
        ))
    }

    private var mediaSession: Any? = null

    // The caller serializes updates on the main thread and handles reflection failures.
    fun setMediaStyleByReflection(
        context: Context,
        builder: NotificationCompat.Builder,
        tag: String,
        pendingIntent: PendingIntent?,
    ) {
        val session = mediaSession ?: mediaSessionClass
            .getConstructor(Context::class.java, String::class.java)
            .newInstance(context.applicationContext, tag)
        try {
            mediaSessionClass.getMethod("setFlags", Int::class.javaPrimitiveType).invoke(session, 3)
            pendingIntent?.let {
                mediaSessionClass.getMethod("setSessionActivity", PendingIntent::class.java).invoke(session, it)
            }
            mediaSessionClass.getMethod("setActive", Boolean::class.javaPrimitiveType).invoke(session, true)

            val token = mediaSessionClass.getMethod("getSessionToken").invoke(session)
            val mediaStyle = mediaStyleClass.getConstructor().newInstance()
            mediaStyleClass.getMethod("setMediaSession", tokenClass).invoke(mediaStyle, token)

            val playbackStateBuilder = playbackStateBuilderClass.getConstructor().newInstance()
            val statePlaying = playbackStateClass.getField("STATE_PLAYING").getInt(null)
            playbackStateBuilderClass.getMethod(
                "setState",
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
                Float::class.javaPrimitiveType,
            ).invoke(playbackStateBuilder, statePlaying, 0L, 1f)
            val playbackState = playbackStateBuilderClass.getMethod("build").invoke(playbackStateBuilder)
            mediaSessionClass.getMethod("setPlaybackState", playbackStateClass).invoke(session, playbackState)

            builder.setStyle(mediaStyle as NotificationCompat.Style)
            mediaSession = session
        } catch (error: Throwable) {
            if (mediaSession !== session) {
                runCatching { mediaSessionClass.getMethod("release").invoke(session) }
            }
            throw error
        }
    }
}
