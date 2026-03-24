package com.sphere.shortvideos.dialogs.withdraw

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.graphics.toColorInt
import androidx.fragment.app.DialogFragment
import com.sphere.shortvideos.R
import com.sphere.shortvideos.databinding.DialogLockInfoBinding
import com.sphere.shortvideos.helper.MoneyCacheHelper
import com.sphere.shortvideos.helper.WithdrawAmountHelper
import com.sphere.shortvideos.view.setColorText

/**
 * Date：2026/3/23
 * Describe: Lock info dialog
 */
class LockInfoDialogFragment : DialogFragment() {
    var onLater: (() -> Unit)? = null
    var onSecure: (() -> Unit)? = null

    private var _binding: DialogLockInfoBinding? = null
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
        _binding = DialogLockInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val money=WithdrawAmountHelper.moneyFormatAddUnit(MoneyCacheHelper.fetchCurMoney())
        val fullText = getString(R.string.lock_info_desc,
            money)
        binding.tvDesc.setColorText(fullText,money,"#FFDD00".toColorInt())
        binding.btnLater.setOnClickListener {
            onLater?.invoke()
            dismissAllowingStateLoss()
        }
        binding.btnSecure.setOnClickListener {
            onSecure?.invoke()
            dismissAllowingStateLoss()
        }
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
