package com.sphere.shortvideos.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.animation.AnimatorSet
import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import androidx.fragment.app.DialogFragment
import com.sphere.shortvideos.R
import com.sphere.shortvideos.databinding.DialogWelcomeBonusBinding
import com.sphere.shortvideos.helper.RewardHelper
import com.sphere.shortvideos.helper.TaskHelper
import com.sphere.shortvideos.helper.mmkv.MMKVRepository

/**
 * Date：2026/1/22
 * Describe: Welcome bonus dialog
 */
class WelcomeBonusDialogFragment : DialogFragment() {

    var onClaim: (() -> Unit)? = null
    var onClose: (() -> Unit)? = null

    private var _binding: DialogWelcomeBonusBinding? = null
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
        _binding = DialogWelcomeBonusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startAnim()
        binding.btnClaim.setOnClickListener {
            onClaim?.invoke()
            MMKVRepository.isNewUser = false
            TaskHelper.addMoney(RewardHelper.getConfig().moneyNewuserGift.reward)
            dismissAllowingStateLoss()
        }
        binding.ivClose.setOnClickListener {
            onClose?.invoke()
            dismissAllowingStateLoss()
        }
    }

    private fun startAnim() {
        binding.ivAnim.alpha = 0.3f
        binding.ivAnim.scaleX = 0.1f
        binding.ivAnim.scaleY = 0.1f
        binding.ivRewardBox.alpha = 0f

        val alphaAnim = ObjectAnimator.ofFloat(binding.ivAnim, View.ALPHA, 0.3f, 1f).apply {
            duration = 1000L
        }

        val scaleX = PropertyValuesHolder.ofKeyframe(
            View.SCALE_X,
            Keyframe.ofFloat(0f, 0.1f),
            Keyframe.ofFloat(0.7f, 1.5f),
            Keyframe.ofFloat(1f, 1f)
        )
        val scaleY = PropertyValuesHolder.ofKeyframe(
            View.SCALE_Y,
            Keyframe.ofFloat(0f, 0.1f),
            Keyframe.ofFloat(0.7f, 1.5f),
            Keyframe.ofFloat(1f, 1f)
        )
        val scaleAnim = ObjectAnimator.ofPropertyValuesHolder(binding.ivAnim, scaleX, scaleY).apply {
            duration = 1300L
        }

        val rewardAlphaAnim = ObjectAnimator.ofFloat(binding.ivRewardBox, View.ALPHA, 0f, 1f).apply {
            duration = 1000L
        }

        AnimatorSet().apply {
            playTogether(alphaAnim, scaleAnim, rewardAlphaAnim)
            start()
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

    companion object {
        fun newInstance() = WelcomeBonusDialogFragment()
    }
}
