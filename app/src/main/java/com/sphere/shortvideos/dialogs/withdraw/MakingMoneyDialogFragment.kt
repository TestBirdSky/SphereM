package com.sphere.shortvideos.dialogs.withdraw

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.graphics.toColorInt
import androidx.fragment.app.DialogFragment
import com.sphere.shortvideos.R
import com.sphere.shortvideos.databinding.DialogMakingMoneyingBinding
import com.sphere.shortvideos.helper.WithdrawAmountHelper
import com.sphere.shortvideos.view.setColorText

/**
 * Date：2026/3/23
 * Describe: Account security restriction dialog
 */
class MakingMoneyDialogFragment : DialogFragment() {
    var onClose: (() -> Unit)? = null
    var onClaim: (() -> Unit)? = null

    private var _binding: DialogMakingMoneyingBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.TransparentMaterialDialog)
        isCancelable = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogMakingMoneyingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ivClose.setOnClickListener {
            onClose?.invoke()
            dismissAllowingStateLoss()
        }
        binding.btnClaim.setOnClickListener {
            onClaim?.invoke()
            dismissAllowingStateLoss()
        }
        val tag = WithdrawAmountHelper.fetchWithdrawMinMoney()
        val text = getString(R.string.tips_mini_str, tag)
        binding.tvDes.setColorText(text, tag, "#32C752".toColorInt())
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
