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
import com.sphere.shortvideos.baseui.setTaskInfo
import com.sphere.shortvideos.databinding.DialogTaskInfoBinding
import com.sphere.shortvideos.helper.WithdrawAmountHelper
import com.sphere.shortvideos.view.AnimViewHelper

/**
 * Date：2026/1/27
 * Describe: Task info dialog
 */
class TaskInfoDialogFragment(val ac: GenericActivity) : DialogFragment() {
    private val con by lazy { WithdrawAmountHelper.fetchCurMoneyAndGetMoneyMinValue().second }

    var onClose: (() -> Unit)? = null

    private var _binding: DialogTaskInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.TransparentMaterialDialog)
        isCancelable = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogTaskInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ivClose.setOnClickListener {
            onClose?.invoke()
            dismissAllowingStateLoss()
        }
        refreshMoney()
        binding.layoutTask.setTaskInfo(ac, receiverMoneyEvent = { m, sourceView ->
            AnimViewHelper.playCoinFlyCopyAnim(sourceView, binding.ivM)
            refreshMoney()
        })
    }

    private fun refreshMoney() {
        val progressText = WithdrawAmountHelper.fetchCurMoneyAndGetMoneyMinValue().let { pair ->
            "${WithdrawAmountHelper.moneyFormatAddUnitWithNoSpace(pair.first)}/${
                WithdrawAmountHelper.moneyFormatAddUnitWithNoSpace(pair.second)
            }"
        }
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
        _binding = null
    }
}
