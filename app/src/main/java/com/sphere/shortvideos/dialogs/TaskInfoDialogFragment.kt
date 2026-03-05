package com.sphere.shortvideos.dialogs

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.sphere.shortvideos.R
import com.sphere.shortvideos.baseui.GenericActivity
import com.sphere.shortvideos.view.setTaskInfo
import com.sphere.shortvideos.databinding.DialogTaskInfoBinding
import com.sphere.shortvideos.helper.HelperRewardShow
import com.sphere.shortvideos.helper.WithdrawAmountHelper
import com.sphere.shortvideos.helper.localEvent
import com.sphere.shortvideos.view.AnimViewHelper

/**
 * Date：2026/1/27
 * Describe: Task info dialog
 *
 * 注意：必须保留无参构造函数，避免 Fragment 重建时反射失败。
 */
class TaskInfoDialogFragment : DialogFragment() {
    var onClose: (() -> Unit)? = null

    private var mBinding: DialogTaskInfoBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.TaskInfoDialogTheme)
        isCancelable = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mBinding = DialogTaskInfoBinding.inflate(inflater, container, false)
        return mBinding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 使用局部变量缓存 binding，避免在回调中访问已被置空的 mBinding 导致 NPE
        val binding = mBinding ?: return
        val hostActivity = activity as? GenericActivity ?: return

        localEvent("billetera_pop")
        binding.ivClose.setOnClickListener {
            onClose?.invoke()
            dismissAllowingStateLoss()
        }
        // 绑定到 viewLifecycleOwner，防止在 onDestroyView 之后仍然回调导致 mBinding 为 null
        HelperRewardShow.curGetMoneyAnimLiveData.observe(viewLifecycleOwner) {
            val currentBinding = mBinding ?: return@observe
            refreshMoney(it.replace("\t", ""), WithdrawAmountHelper.fetchWithdrawMinMoney())
        }
        binding.layoutTask.setTaskInfo(hostActivity, receiverMoneyEvent = { reward, sourceView ->
            val currentBinding = mBinding ?: return@setTaskInfo
            AnimViewHelper.playCoinFlyCopyAnim(sourceView, currentBinding.ivM, end = {
                if (reward > 0) {
                    HelperRewardShow.addMoneyNotExChange(reward)
                }
            })
        })
    }

    private fun refreshMoney(moneyCur: String, minWithMoney: String) {
        val binding = mBinding ?: return
        val progressText = "$moneyCur/$minWithMoney"
        binding.tvProgress.text = progressText
        binding.progressView.setProgress(WithdrawAmountHelper.fetchGetMoneyProgress(), true)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.attributes = window.attributes.apply { gravity = Gravity.BOTTOM }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }
}
