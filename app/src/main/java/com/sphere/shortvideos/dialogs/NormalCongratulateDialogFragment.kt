package com.sphere.shortvideos.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.sphere.shortvideos.R
import com.sphere.shortvideos.databinding.DialogCongratulateBinding
import com.sphere.shortvideos.helper.MoneyCacheHelper
import com.sphere.shortvideos.helper.SoundHelper
import com.sphere.shortvideos.helper.WithdrawAmountHelper
import com.sphere.shortvideos.helper.localEvent
import com.sphere.shortvideos.view.AnimViewHelper

/**
 * Date：2026/1/23
 * Describe: Congratulate dialog
 */
class NormalCongratulateDialogFragment : DialogFragment() {

    var onClaim: ((Double) -> Unit)? = null
    var onClose: ((Double) -> Unit)? = null
    var onNormalClick: ((Double) -> Unit)? = null

    private var _binding: DialogCongratulateBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.TransparentMaterialDialog)
        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogCongratulateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SoundHelper.playDialogAppear(requireContext()) // 弹窗出现音效
        AnimViewHelper.playWelcomeBonusAnim(binding.ivAnim, binding.ivRewardBox)
        AnimViewHelper.playCelebrateAnim(binding.ivAnim2, 1000L)
        val adRvReward = MoneyCacheHelper.fetchRvAdReward()
        val curMoney = WithdrawAmountHelper.fetchCurMoneyAndGetMoneyMinValue()
        val des =
            WithdrawAmountHelper.moneyFormatAddUnitWithNoSpace(curMoney.first) + "/" + WithdrawAmountHelper.moneyFormatAddUnitWithNoSpace(
                curMoney.second)
        binding.tvPro.text = des
        binding.progressView.progress = WithdrawAmountHelper.fetchGetMoneyProgress()
        binding.tvRewardValue.text = adRvReward.second
        binding.ivClose.setOnClickListener {
            onClose?.invoke(adRvReward.first)
            localEvent("money_pop_close")
            dismissAllowingStateLoss()
        }
        binding.btnNormal.setOnClickListener {
            onNormalClick?.invoke(adRvReward.first)
            dismissAllowingStateLoss()
            localEvent("money_pop_1x")
        }
        AnimViewHelper.applyPressBounceEffect(binding.btnClaim)
        binding.btnClaim.setOnClickListener {
            localEvent("money_pop_2x")
            onClaim?.invoke(adRvReward.first * 2)
            dismissAllowingStateLoss()
        }
        localEvent("money_pop")
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
