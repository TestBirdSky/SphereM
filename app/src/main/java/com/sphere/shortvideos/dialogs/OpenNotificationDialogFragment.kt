package com.sphere.shortvideos.dialogs

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import androidx.fragment.app.DialogFragment
import com.sphere.shortvideos.R
import com.sphere.shortvideos.databinding.DialogOpenNotificationBinding
import com.sphere.shortvideos.helper.MoneyCacheHelper

/**
 * Date：2026/1/23
 * Describe: Open notification dialog
 */
class OpenNotificationDialogFragment : DialogFragment() {

    var onClaim: ((Double) -> Unit)? = null
    var onClose: (() -> Unit)? = null

    private var _binding: DialogOpenNotificationBinding? = null
    private val binding get() = _binding!!

    private var swingAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.TransparentMaterialDialog)
        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogOpenNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startSwingAnim()
        val rewardValue = MoneyCacheHelper.fetchPushReward()
        binding.tvMoney.text = getString(R.string.allow_get) + "\t${rewardValue.second}${rewardValue.first}"
        binding.ivClose.setOnClickListener {
            onClose?.invoke()
            dismissAllowingStateLoss()
        }
        binding.btnClaim.setOnClickListener {
            onClaim?.invoke(rewardValue.first)
            dismissAllowingStateLoss()
        }
    }

    private fun startSwingAnim() {
        if (swingAnimator != null) return
        binding.ivAnim.rotation = 0f
        swingAnimator = ObjectAnimator.ofFloat(binding.ivAnim, View.ROTATION, -15f, 30f).apply {
            duration = 1600L
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
        }
    }

    private fun stopSwingAnim() {
        swingAnimator?.cancel()
        swingAnimator = null
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
        stopSwingAnim()
        super.onDestroyView()
        _binding = null
    }

}
