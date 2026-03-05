package com.sphere.shortvideos.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.sphere.shortvideos.R
import com.sphere.shortvideos.databinding.DialogWelcomeBonusBinding
import com.sphere.shortvideos.helper.HelperRewardShow
import com.sphere.shortvideos.helper.MoneyCacheHelper
import com.sphere.shortvideos.helper.SoundHelper
import com.sphere.shortvideos.helper.reward.RewardHelper
import com.sphere.shortvideos.view.AnimViewHelper

/**
 * Date：2026/1/22
 * Describe: Welcome bonus dialog
 */
class WelcomeBonusDialogFragment : DialogFragment() {
    var onDismissCall: ((Double) -> Unit)? = null

    private var _binding: DialogWelcomeBonusBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.TransparentMaterialDialog)
        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogWelcomeBonusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SoundHelper.playDialogAppear(requireContext()) // 弹窗出现音效
        AnimViewHelper.playWelcomeBonusAnim(binding.ivAnim, binding.ivRewardBox)
        AnimViewHelper.playCelebrateAnim(binding.ivAnim2, 1000L)
        val config = RewardHelper.getConfigByLanguage()
        val r = config.getRewardNewUser()
        binding.tvRewardValue.text = r.second
        binding.btnClaim.setOnClickListener {
            onDismissCall?.invoke(r.first)
            dismissAllowingStateLoss()
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
