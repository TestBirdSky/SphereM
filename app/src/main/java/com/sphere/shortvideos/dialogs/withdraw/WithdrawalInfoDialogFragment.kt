package com.sphere.shortvideos.dialogs.withdraw

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.sphere.shortvideos.R
import com.sphere.shortvideos.databinding.DialogWithdrawalInfoBinding
import com.sphere.shortvideos.helper.WithdrawAmountHelper
import com.sphere.shortvideos.helper.withdraw.WithdrawalActionHelper
import com.sphere.shortvideos.helper.withdraw.db.WithdrawalRecordStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 提现信息确认弹窗（Confirm 与关闭按钮共用同一个回调）。
 */
class WithdrawalInfoDialogFragment : DialogFragment() {
    var onAction: (() -> Unit)? = null

    private var _binding: DialogWithdrawalInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.TransparentMaterialDialog)
        isCancelable = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogWithdrawalInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fillDefaultInfo()
        persistWithdrawalRecord()
        binding.btnConfirm.setOnClickListener {
            onAction?.invoke()
            dismissAllowingStateLoss()
        }
        binding.ivClose.setOnClickListener {
            onAction?.invoke()
            dismissAllowingStateLoss()
        }
    }

    /**
     * 外部可覆盖内容；未调用时展示默认文案。
     */

    private fun fillDefaultInfo() {
        val today = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())
        binding.tvAmount.text =
            WithdrawAmountHelper.moneyFormatAddUnitWithNoSpace(WithdrawalActionHelper.withdrawalValue)
        binding.tvPlatformValue.text = getString(R.string.app_name)
        binding.tvInstructionValue.text = getString(R.string.withdraw_info_default_instruction)
        binding.tvCreationTimeValue.text = today
        binding.tvAccountValue.text = WithdrawalActionHelper.accountWithdrawal
        binding.ivMethodValue.setImageResource(WithdrawAmountHelper.findWithdrawPaymentMethodById(WithdrawalActionHelper.withdrawalMethodId).iconSelected)

    }

    private fun persistWithdrawalRecord() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            WithdrawalRecordStore.createRecordFromCache(initialProgress = 0.1)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
        dialog?.setCanceledOnTouchOutside(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
