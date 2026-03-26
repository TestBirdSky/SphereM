package com.sphere.shortvideos.helper

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.sphere.shortvideos.R

/**
 * 短音效：金币收缩、弹窗出现、翻倍滚动
 */
object SoundHelper {

    private var soundPool: SoundPool? = null
    private var soundIdCoinShrink: Int = 0
    private var soundIdDialogAppear: Int = 0
    private var soundIdDoubleRoll: Int = 0
    private var soundIdWithdrawalMoney: Int = 0
    private var soundIdWaringTips: Int = 0

    /**
     * 建议在 Application.onCreate 中调用，预加载音效
     */
    fun init(context: Context) {
        if (soundPool != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attrs)
            .build()
        val pool = soundPool ?: return
        soundIdCoinShrink = pool.load(context, R.raw.get_money, 1)
        soundIdDialogAppear = pool.load(context, R.raw.show_dialog, 1)
        soundIdDoubleRoll = pool.load(context, R.raw.whell, 1)
        soundIdWithdrawalMoney = pool.load(context, R.raw.withdrawal_money, 1)
        soundIdWaringTips = pool.load(context, R.raw.waring_tips, 1)
    }

    private fun ensureInit(context: Context) {
        if (soundPool == null) init(context.applicationContext)
    }

    /** 金币收缩/金币增加时播放 */
    fun playCoinShrink(context: Context) {
        ensureInit(context)
        soundPool?.play(soundIdCoinShrink, 1f, 1f, 0, 0, 1f)
    }

    /** 弹窗出现时播放 */
    fun playDialogAppear(context: Context) {
        ensureInit(context)
        soundPool?.play(soundIdDialogAppear, 1f, 1f, 0, 0, 1f)
    }

    /** 翻倍滚动时播放 */
    fun playDoubleRoll(context: Context) {
        ensureInit(context)
        soundPool?.play(soundIdDoubleRoll, 1f, 1f, 0, 0, 1f)
    }

    /** 提现相关弹窗出现时播放 */
    fun playWithdrawalMoney(context: Context) {
        ensureInit(context)
        soundPool?.play(soundIdWithdrawalMoney, 1f, 1f, 0, 0, 1f)
    }

    /** 风险/提示类弹窗出现时播放 */
    fun playWaringTips(context: Context) {
        ensureInit(context)
        soundPool?.play(soundIdWaringTips, 1f, 1f, 0, 0, 1f)
    }
}
