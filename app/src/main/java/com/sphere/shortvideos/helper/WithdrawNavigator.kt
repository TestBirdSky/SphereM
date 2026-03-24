package com.sphere.shortvideos.helper

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.sphere.shortvideos.GlobalConstants
import com.sphere.shortvideos.activity.MainActivity
import com.sphere.shortvideos.activity.PangleDramaPlayActivity

/**
 * 统一跳转到主界面提现页 [WithdrawFragment]（底部 Wallet Tab）。
 *
 * - 已在 [MainActivity]：直接 [MainActivity.jumpWallet]
 * - 其它页面（如 [PangleDramaPlayActivity]）：`FLAG_ACTIVITY_CLEAR_TOP` 唤起 Main，并带 [GlobalConstants.EXTRA_KEY_OPEN_WALLET]；
 *   从全屏播放页离开时会 [finish] 播放页，避免返回栈过深。
 */
object WithdrawNavigator {

    fun navigateToWithdrawTab(activity: FragmentActivity) {
        when (activity) {
            is MainActivity -> activity.jumpWallet()
            is PangleDramaPlayActivity -> activity.stopPlaybackAndOpenWallet()
            else -> {
                activity.startActivity(
                    Intent(activity, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra(GlobalConstants.EXTRA_KEY_OPEN_WALLET, true)
                    }
                )
            }
        }
    }
}
