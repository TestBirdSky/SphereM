package com.sphere.shortvideos.helper

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * DialogFragment 展示通用工具：记录当前是否有弹窗在展示、获取当前实例，并统一 [show] 入口。
 *
 * **注意**
 * - 进程内只维护**最后一次**通过本类 [show] 登记的实例；多 Activity 并存时请以各自 [FragmentManager] 调用。
 * - 切换弹窗时会先解除对旧实例的销毁监听，再登记新实例，避免旧实例晚到的 [onDestroy] 把新引用清空。
 */
object DialogFragmentDisplayHelper {

    @Volatile
    private var currentDialog: DialogFragment? = null

    private var destroyObserver: DefaultLifecycleObserver? = null

    /** 当前是否有正在展示的 Dialog（已 add 且 Window 处于 showing） */
    fun isShowing(): Boolean {
        val d = currentDialog ?: return false
        if (!d.isAdded) return false
        return runCatching { d.dialog?.isShowing == true }.getOrDefault(false)
    }

    /**
     * 指定 [FragmentActivity] 下（含嵌套子 [Fragment]）是否有任一 [DialogFragment] 正在展示。
     * 与是否通过本类 [show] 登记无关，凡挂在该 Activity [FragmentManager] 树上的弹窗均会检测到。
     */
    @JvmStatic
    fun hasDialogFragmentShowing(activity: FragmentActivity): Boolean {
        return hasDialogFragmentShowing(activity.supportFragmentManager)
    }

    @JvmStatic
    fun hideCurShowFragment(activity: FragmentActivity) {
        val fm = activity.supportFragmentManager
        if (fm.isStateSaved) {
            fm.fragments.toList().forEach { fragment ->
                if (fragment is DialogFragment && fragment.isAdded) {
                    fragment.dismissAllowingStateLoss()
                }
            }
            return
        }
        fm.fragments.toList().forEach { fragment ->
            if (fragment is DialogFragment && fragment.isAdded) {
                fragment.dismiss()
            }
        }
    }

    /**
     * 指定 [FragmentManager] 为根的子树中是否有 [DialogFragment] 正在展示（含子 Fragment 的 childFragmentManager）。
     */
    @JvmStatic
    fun hasDialogFragmentShowing(fragmentManager: FragmentManager): Boolean {
        for (fragment in fragmentManager.fragments.toList()) {
            if (fragment is DialogFragment && fragment.isAdded) {
                val showing = runCatching { fragment.dialog?.isShowing == true }.getOrDefault(false)
                if (showing) return true
            }
            if (hasDialogFragmentShowingInChildSafe(fragment)) {
                return true
            }
        }
        return false
    }

    /** 统计指定 [FragmentManager] 树中当前正在显示的 [DialogFragment] 数量（含子树）。 */
    @JvmStatic
    fun countShowingDialogFragments(fragmentManager: FragmentManager): Int {
        return countShowingDialogsInternal(fragmentManager) { true }
    }

    /** 统计指定 [FragmentManager] 树中当前正在显示且会触发视频暂停的 Dialog 数量（含子树）。 */
    @JvmStatic
    fun countShowingPauseDialogFragments(fragmentManager: FragmentManager): Int {
        return countShowingDialogsInternal(fragmentManager) { dialog ->
            HelperRewardShow.isPauseFragment(dialog)
        }
    }

    /** 当前登记的 [DialogFragment]；若未通过本类 [show] 展示则可能为 null */
    fun getCurrentDialog(): DialogFragment? = currentDialog

    /**
     * 展示指定 [DialogFragment]。
     *
     * @param tag [FragmentManager] 事务用 tag，默认类简单名
     * @param dismissCurrent 为 true 时先关闭当前登记的弹窗再展示
     * @param executePendingAfterDismiss 在关闭当前弹窗后是否 [FragmentManager.executePendingTransactions]，减少连续事务冲突
     * @return 已有弹窗且 [dismissCurrent] 为 false 时返回 false，未执行展示；否则 true
     */
    @JvmOverloads
    fun show(
        fragmentManager: FragmentManager,
        dialog: DialogFragment,
        tag: String? = null,
        dismissCurrent: Boolean = true,
        executePendingAfterDismiss: Boolean = true,
    ): Boolean {
        if (isShowing() && !dismissCurrent) {
            return false
        }

        if (dismissCurrent) {
            currentDialog?.dismissAllowingStateLoss()
            if (executePendingAfterDismiss) {
                runCatching { fragmentManager.executePendingTransactions() }
            }
        }

        detachObserverFromCurrent()
        currentDialog = dialog
        attachDestroyObserver(dialog)

        val showTag = tag ?: dialog::class.java.simpleName
        if (!dialog.isAdded) {
            dialog.show(fragmentManager, showTag)
        }
        return true
    }

    /** 关闭当前登记的弹窗（若存在） */
    fun dismissCurrent() {
        currentDialog?.dismissAllowingStateLoss()
    }

    /**
     * 若当前引用等于 [dialog] 则解除监听并清空（一般无需调用；对话框正常销毁时会自动清理）
     */
    fun clearReferenceIf(dialog: DialogFragment) {
        if (currentDialog === dialog) {
            detachObserverFromCurrent()
            currentDialog = null
        }
    }

    private fun attachDestroyObserver(dialog: DialogFragment) {
        val observer = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                dialog.lifecycle.removeObserver(this)
                if (destroyObserver === this) {
                    destroyObserver = null
                }
                if (currentDialog === dialog) {
                    currentDialog = null
                }
            }
        }
        destroyObserver = observer
        dialog.lifecycle.addObserver(observer)
    }

    private fun detachObserverFromCurrent() {
        val d = currentDialog ?: return
        val obs = destroyObserver
        if (obs != null) {
            runCatching { d.lifecycle.removeObserver(obs) }
        }
        destroyObserver = null
    }

    private fun countShowingDialogsInternal(
        fragmentManager: FragmentManager,
        matcher: (DialogFragment) -> Boolean,
    ): Int {
        var count = 0
        // snapshots：避免遍历期间 Fragment 事务修改 mAdded 引发 ConcurrentModificationException
        for (fragment in fragmentManager.fragments.toList()) {
            if (fragment is DialogFragment && fragment.isAdded && matcher(fragment)) {
                val showing = runCatching { fragment.dialog?.isShowing == true }.getOrDefault(false)
                if (showing) count++
            }
            count += countShowingDialogsInChildSafe(fragment, matcher)
        }
        return count
    }

    /**
     * ViewPager2 / 事务中间态下可能出现 isAdded 为 true 但 getChildFragmentManager 仍抛
     * [IllegalStateException]（has not been attached yet），此处必须兜底。
     */
    private fun hasDialogFragmentShowingInChildSafe(parent: Fragment): Boolean =
        runCatching {
            if (!parent.isAdded) return false
            hasDialogFragmentShowing(parent.childFragmentManager)
        }.getOrDefault(false)

    private fun countShowingDialogsInChildSafe(parent: Fragment, matcher: (DialogFragment) -> Boolean): Int =
        runCatching {
            if (!parent.isAdded) return@runCatching 0
            countShowingDialogsInternal(parent.childFragmentManager, matcher)
        }.getOrDefault(0)
}
