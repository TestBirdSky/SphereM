package com.sphere.shortvideos.fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.sphere.shortvideos.R
import com.sphere.shortvideos.adapter.WithdrawAmountAdapter
import com.sphere.shortvideos.adapter.WithdrawMethodAdapter
import com.sphere.shortvideos.baseui.GenericFragment
import com.sphere.shortvideos.databinding.FragmentWallteBinding
import com.sphere.shortvideos.databinding.LayoutWithdrawalActionBinding
import com.sphere.shortvideos.dialogs.withdraw.FlipCardDialogFragment
import com.sphere.shortvideos.dialogs.withdraw.WithdrawApplyTransitionDialogFragment
import com.sphere.shortvideos.helper.DialogFragmentDisplayHelper
import com.sphere.shortvideos.helper.MoneyCacheHelper
import com.sphere.shortvideos.helper.WithdrawAmountHelper
import com.sphere.shortvideos.helper.localEvent
import com.sphere.shortvideos.helper.withdraw.LayoutWithdrawalActionController
import com.sphere.shortvideos.helper.withdraw.WithdrawUserInfoMarqueeController
import com.sphere.shortvideos.helper.withdraw.WithdrawalActionHelper
import com.sphere.shortvideos.view.AnimViewHelper

/**
 * Date：2026/1/21
 * Describe:
 */
class WithdrawFragment : GenericFragment<FragmentWallteBinding>() {
    private val methodAdapter = WithdrawMethodAdapter()
    private val amountAdapter = WithdrawAmountAdapter()
    private val userInfoMarqueeController = WithdrawUserInfoMarqueeController(this)
    private var withdrawalActionController: LayoutWithdrawalActionController? = null

    override fun bindView(inflater: LayoutInflater, container: ViewGroup?): FragmentWallteBinding {
        return FragmentWallteBinding.inflate(inflater, container, false)
    }

    override fun initUI() {
        setupWithdrawMethods()
        setupWithdrawalAmountSection()
        userInfoMarqueeController.setup(binding.layoutWithUserInfo)
        AnimViewHelper.applyPressBounceEffect(binding.tvWithdraw)
        binding.tvWithdraw.setOnClickListener {
            withdrawalActionController?.let { c ->
                if (!binding.tvWithdraw.isEnabled) return@setOnClickListener
                runCatching {
                    c.handleMainWithdrawClick(parentFragmentManager)
//                    DialogFragmentDisplayHelper.show(parentFragmentManager, WithdrawApplyTransitionDialogFragment())
//                    DialogFragmentDisplayHelper.show(parentFragmentManager, FlipCardDialogFragment())
                }
                return@setOnClickListener
            }
            val cur3 = MoneyCacheHelper.fetchCurMoney()
            val m = amountAdapter.fetchWithdrawMoney()
            localEvent("withdraw_withdraw")
            if (cur3 < m) {
                Toast.makeText(context, getString(R.string.cant_with_tips), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, getString(R.string.withdraw_wait_tips), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupWithdrawMethods() {
        binding.rvWithdraw.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        methodAdapter.setMethods(WithdrawAmountHelper.fetchWithdrawPaymentMethods())
        binding.rvWithdraw.adapter = methodAdapter
    }

    private fun setupWithdrawAmounts() {
        binding.rvWithdrawMoney.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvWithdrawMoney.adapter = amountAdapter
        amountAdapter.submitList(WithdrawAmountHelper.fetchWithdrawAmounts())
    }

    private fun setupWithdrawalAmountSection() {
        withdrawalActionController?.detach()
        withdrawalActionController = null
        val openWithdrawAction = WithdrawalActionHelper.getConfig().isOpenWithdraw()
        if (openWithdrawAction) {
            binding.layoutWa.visibility = View.GONE
            binding.viewParent.removeAllViews()
            val actionBinding = LayoutWithdrawalActionBinding.inflate(layoutInflater, binding.viewParent, false)
            binding.viewParent.addView(actionBinding.root)
            withdrawalActionController = LayoutWithdrawalActionController(this, actionBinding).also {
                it.attach(binding.tvWithdraw)
            }
            return
        }
        binding.layoutWa.visibility = View.VISIBLE
        binding.tvWithdraw.isEnabled = true
        binding.tvWithdraw.alpha = 1f
        binding.viewParent.removeAllViews()
        setupWithdrawAmounts()
    }

    override fun onDestroyView() {
        withdrawalActionController?.detach()
        withdrawalActionController = null
        userInfoMarqueeController.stop()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        localEvent("withdraw_page")
        binding.tvMoney.text = WithdrawAmountHelper.moneyFormatAddUnit(MoneyCacheHelper.fetchCurMoney())
        withdrawalActionController?.run {
            refreshMinTips()
            refreshMainWithdrawButton(binding.tvWithdraw)
        }
    }
}