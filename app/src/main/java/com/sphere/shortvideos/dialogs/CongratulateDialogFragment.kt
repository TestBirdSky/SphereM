package com.sphere.shortvideos.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.sphere.shortvideos.R
import com.sphere.shortvideos.databinding.DialogCongratulateBinding
import com.sphere.shortvideos.view.AnimViewHelper

/**
 * Date：2026/1/23
 * Describe: Congratulate dialog
 */
class CongratulateDialogFragment : DialogFragment() {

    var onClaim: (() -> Unit)? = null
    var onClose: (() -> Unit)? = null

    private var _binding: DialogCongratulateBinding? = null
    private val binding get() = _binding!!

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
        _binding = DialogCongratulateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnimViewHelper.playWelcomeBonusAnim(binding.ivAnim, binding.ivRewardBox)
        AnimViewHelper.slideInFromTop(binding.ivAnim2, 1200L)
        binding.ivClose.setOnClickListener {
            onClose?.invoke()
            dismissAllowingStateLoss()
        }
        binding.btnClaim.setOnClickListener {
            onClaim?.invoke()
            dismissAllowingStateLoss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
        dialog?.setCanceledOnTouchOutside(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
