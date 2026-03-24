package com.sphere.shortvideos.dialogs

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.sphere.shortvideos.R
import com.sphere.shortvideos.adapter.WithdrawMethodAdapter
import com.sphere.shortvideos.databinding.DialogPaymentInformationBinding
import com.sphere.shortvideos.helper.WithdrawAmountHelper
import com.sphere.shortvideos.helper.withdraw.WithdrawPaymentAccountHints

/**
 * 收款信息弹窗：横向提现方式与钱包页 [fragment_wallte] 同源（[WithdrawAmountHelper.fetchWithdrawPaymentMethods]）。
 * 根据所选渠道切换「账户类型」标题与输入 Hint；输入为空时提交按钮不可点。
 * 提交后的持久化请在类内 [binding.btnSubmit] 点击处自行实现。
 */
class PaymentInformationDialogFragment : DialogFragment() {

    private var _binding: DialogPaymentInformationBinding? = null
    private val binding get() = _binding!!

    private val methodAdapter = WithdrawMethodAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.TransparentMaterialDialog)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPaymentInformationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvWithdraw.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvWithdraw.adapter = methodAdapter
        methodAdapter.setMethods(WithdrawAmountHelper.fetchWithdrawPaymentMethods())
        methodAdapter.onItemClick = {
            applyFieldCopyForSelectedMethod()
        }
        applyFieldCopyForSelectedMethod()

        binding.etAccount.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    updateSubmitEnabled(s)
                }
            }
        )
        updateSubmitEnabled(binding.etAccount.text)

        binding.btnSubmit.setOnClickListener {
            val method = methodAdapter.getSelectedMethod() ?: return@setOnClickListener
            val account = binding.etAccount.text?.toString()?.trim().orEmpty()
            if (account.isEmpty()) return@setOnClickListener
            // TODO: 在此保存 method（含 name / 图标资源）与 account
            dismissAllowingStateLoss()
        }

        binding.ivClose.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    private fun applyFieldCopyForSelectedMethod() {
        val method = methodAdapter.getSelectedMethod() ?: return
        val copy = WithdrawPaymentAccountHints.forMethod(method.name)
        binding.tvFieldLabel.text = copy.label
        binding.etAccount.hint = copy.hint
    }

    private fun updateSubmitEnabled(s: Editable?) {
        val text = s?.toString().orEmpty().trim()
        binding.btnSubmit.isEnabled = text.isNotEmpty()
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
