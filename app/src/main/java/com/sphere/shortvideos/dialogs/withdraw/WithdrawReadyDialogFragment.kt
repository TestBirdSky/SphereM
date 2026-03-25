package com.sphere.shortvideos.dialogs.withdraw

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.sphere.shortvideos.R
import com.sphere.shortvideos.activity.MainActivity
import com.sphere.shortvideos.databinding.DialogWithdrawReadyBinding
import com.sphere.shortvideos.helper.MoneyCacheHelper
import com.sphere.shortvideos.helper.WithdrawAmountHelper
import com.sphere.shortvideos.helper.WithdrawNavigator
import com.sphere.shortvideos.helper.withdraw.WithdrawalActionHelper
import com.sphere.shortvideos.helper.localEvent
import com.sphere.shortvideos.helper.mmkv.MMKVRepository
import com.sphere.shortvideos.view.AnimViewHelper

/**
 * 达到提现门槛时的「里程碑」提醒弹窗。
 *
 * 展示时机由业务调用 [Companion.showIfEligible]；「Claim」通过 [WithdrawNavigator] 跳转提现 Tab
 * （在 [PangleDramaPlayActivity] 等页面会先回到 [MainActivity] 并可选结束播放页）。
 */
class WithdrawReadyDialogFragment : DialogFragment() {

    private var _binding: DialogWithdrawReadyBinding? = null
    private val binding get() = _binding!!

    private val illustrationBgAnimators = mutableListOf<Animator>()

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
        _binding = DialogWithdrawReadyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindMessage()
        startIllustrationBgAnim()
        AnimViewHelper.applyPressBounceEffect(binding.btnClaim)
        binding.btnLater.setOnClickListener {
            dismissAllowingStateLoss()
        }
        binding.btnClaim.setOnClickListener {
            val act = activity ?: return@setOnClickListener
            WithdrawNavigator.navigateToWithdrawTab(act)
            if (act is MainActivity) {
                dismissAllowingStateLoss()
            }
        }
    }

    /** 背景图持续旋转 + 呼吸缩放 */
    private fun startIllustrationBgAnim() {
        stopIllustrationBgAnim()
        val v = binding.ivBgAnim
        val rotate = ObjectAnimator.ofFloat(v, View.ROTATION, 0f, 360f).apply {
            duration = 12_000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
        }
        val scaleX = ObjectAnimator.ofFloat(v, View.SCALE_X, 0.88f, 1.1f).apply {
            duration = 2_000L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        val scaleY = ObjectAnimator.ofFloat(v, View.SCALE_Y, 0.88f, 1.1f).apply {
            duration = 2_000L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        illustrationBgAnimators.add(rotate)
        illustrationBgAnimators.add(scaleX)
        illustrationBgAnimators.add(scaleY)
        rotate.start()
        scaleX.start()
        scaleY.start()
    }

    private fun stopIllustrationBgAnim() {
        illustrationBgAnimators.forEach { it.cancel() }
        illustrationBgAnimators.clear()
        _binding?.ivBgAnim?.rotation = 0f
        _binding?.ivBgAnim?.scaleX = 1f
        _binding?.ivBgAnim?.scaleY = 1f
    }

    private fun bindMessage() {
        val amountStr = WithdrawAmountHelper.moneyFormatAddUnit(MoneyCacheHelper.fetchCurMoney()).replace("\t", " ")
        val template = getString(R.string.withdraw_ready_message, amountStr)
        val ss = SpannableStringBuilder(template)
        val start = template.indexOf(amountStr)
        if (start >= 0) {
            ss.setSpan(
                ForegroundColorSpan(Color.parseColor("#F5B71A")),
                start,
                start + amountStr.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        binding.tvMessage.text = ss
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            window.setBackgroundDrawableResource(R.color.color_dialog)
        }
        dialog?.setCanceledOnTouchOutside(false)
    }

    override fun onDestroyView() {
        stopIllustrationBgAnim()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG_FM = "WithdrawReadyDialog"

        /**
         * 当前可提现且远程开关允许提现流程时尝试展示；**全应用生命周期仅展示一次**（MMKV 持久化），
         * 或已展示同 tag 时跳过。
         */
        @JvmStatic
        fun showIfEligible(fragmentManager: FragmentManager) {
            if (WithdrawalActionHelper.getConfig().isOpenWithdraw().not()) return
            if (WithdrawAmountHelper.isCanWithdraw().not()) return
            if (fragmentManager.findFragmentByTag(TAG_FM) != null) return
            if (MMKVRepository.hasShownWithdrawReadyDialogEver) return
            WithdrawReadyDialogFragment().show(fragmentManager, TAG_FM)
            MMKVRepository.hasShownWithdrawReadyDialogEver = true
        }
    }
}
