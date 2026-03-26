package com.sphere.shortvideos.dialogs.withdraw

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.sphere.shortvideos.R
import com.sphere.shortvideos.activity.MainActivity
import com.sphere.shortvideos.databinding.DialogValuedPlayersBinding
import com.sphere.shortvideos.helper.HelperRewardShow
import com.sphere.shortvideos.helper.LauageTools
import com.sphere.shortvideos.helper.MoneyCacheHelper
import com.sphere.shortvideos.helper.WithdrawAmountHelper
import com.sphere.shortvideos.helper.localEvent
import com.sphere.shortvideos.view.AnimViewHelper

/**
 * 优质用户激励弹窗：展示当前余额、距提现门槛差额、提现进度等。
 *
 * - 主金额 / Earned Bonus：当前余额 [MoneyCacheHelper.fetchCurMoney]
 * - Pending Bonus：距最低提现档还差金额（与 [WithdrawAmountHelper.fetchCurMoneyAndGetMoneyMinValue] 门槛一致）
 * - 副文案：字符串资源 [R.string.valued_players_just_away*]，%s 为差额格式化文案；渠道按地区固定为
 *   巴西 PIX、印尼 GoPay、其余 PayPal（见 [justAwayStringRes]）
 */
class ValuedPlayersDialogFragment : DialogFragment() {

    private var _binding: DialogValuedPlayersBinding? = null
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
        _binding = DialogValuedPlayersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        localEvent("valued_players_pop")
        localEvent("premium_user")
        refreshAll()
        HelperRewardShow.curGetMoneyAnimLiveData.observe(viewLifecycleOwner) {
            refreshAll()
        }
        AnimViewHelper.applyPressBounceEffect(binding.btnTapGetCash)
        binding.btnTapGetCash.setOnClickListener {
            localEvent("premium_user_c")
            (activity as? MainActivity)?.jumpToVideoTab()
            dismissAllowingStateLoss()
        }
        binding.ivClose.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    private fun refreshAll() {
        val b = _binding ?: return
        val cur = MoneyCacheHelper.fetchCurMoney()
        val pair = WithdrawAmountHelper.fetchCurMoneyAndGetMoneyMinValue()
        val threshold = pair.second
        val gap = (threshold - cur).coerceAtLeast(0.0)

        b.tvMainAmount.text = WithdrawAmountHelper.moneyFormatAddUnit(cur)
        b.tvEarnedValue.text = WithdrawAmountHelper.moneyFormatAddUnitWithNoSpace(cur)
        b.tvPendingValue.text = WithdrawAmountHelper.moneyFormatAddUnitWithNoSpace(gap)
        b.tvProgressValue.text =
            "${WithdrawAmountHelper.moneyFormatAddUnitWithNoSpace(cur)}/${WithdrawAmountHelper.moneyFormatAddUnitWithNoSpace(threshold)}"
        val p = WithdrawAmountHelper.fetchGetMoneyProgress().coerceIn(0, 100)
        b.progressWithdraw.progress = p
        b.tvJustAway.text = buildJustAwaySpan(gap)
    }

    private fun buildJustAwaySpan(gap: Double): CharSequence {
        val gapStr = WithdrawAmountHelper.moneyFormatAddUnitWithNoSpace(gap)
        val template = getString(justAwayStringRes(), gapStr)
        val ss = SpannableStringBuilder(template)
        val start = template.indexOf(gapStr)
        if (start >= 0) {
            ss.setSpan(
                ForegroundColorSpan(Color.parseColor("#F5B71A")),
                start,
                start + gapStr.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return ss
    }

    /** 巴西 → PIX；印尼 → GoPay；其余（含英语、西语等）→ PayPal */
    private fun justAwayStringRes(): Int = when {
        LauageTools.isBrazil() -> R.string.valued_players_just_away_br
        LauageTools.isIndonesia() -> R.string.valued_players_just_away_id
        else -> R.string.valued_players_just_away
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
