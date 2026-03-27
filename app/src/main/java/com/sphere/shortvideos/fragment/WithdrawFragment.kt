package com.sphere.shortvideos.fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.sphere.shortvideos.R
import com.sphere.shortvideos.adapter.WithdrawAmountAdapter
import com.sphere.shortvideos.adapter.WithdrawMethodAdapter
import com.sphere.shortvideos.adapter.WithdrawalCutInAdapter
import com.sphere.shortvideos.baseui.GenericActivity
import com.sphere.shortvideos.baseui.GenericFragment
import com.sphere.shortvideos.databinding.FragmentWallteBinding
import com.sphere.shortvideos.databinding.ItemWithdrawalTaskBinding
import com.sphere.shortvideos.databinding.LayoutWithdrawalActionBinding
import com.sphere.shortvideos.databinding.WithdrawalLayoutTaskBinding
import com.sphere.shortvideos.dialogs.withdraw.FlipCardDialogFragment
import com.sphere.shortvideos.dialogs.withdraw.MyAccountDialogFragment
import com.sphere.shortvideos.dialogs.withdraw.WithdrawApplyTransitionDialogFragment
import com.sphere.shortvideos.dialogs.withdraw.WithdrawalTaskItem
import com.sphere.shortvideos.dialogs.withdraw.WithdrawalTaskFragment
import com.sphere.shortvideos.helper.DialogFragmentDisplayHelper
import com.sphere.shortvideos.helper.HelperRewardShow
import com.sphere.shortvideos.helper.MoneyCacheHelper
import com.sphere.shortvideos.helper.WithdrawAmountHelper
import com.sphere.shortvideos.helper.ad.AdUtils
import com.sphere.shortvideos.helper.localEvent
import com.sphere.shortvideos.helper.withdraw.LayoutWithdrawalActionController
import com.sphere.shortvideos.helper.withdraw.WithdrawUserInfoMarqueeController
import com.sphere.shortvideos.helper.withdraw.TASK1_STEP
import com.sphere.shortvideos.helper.withdraw.TASK2_STEP
import com.sphere.shortvideos.helper.withdraw.TASK3_STEP
import com.sphere.shortvideos.helper.withdraw.WithdrawalActionHelper
import com.sphere.shortvideos.helper.withdraw.WithdrawalStatus
import com.sphere.shortvideos.view.AnimViewHelper
import com.sphere.shortvideos.vm.WithdrawViewModel
import kotlinx.coroutines.launch

/**
 * Date：2026/1/21
 * Describe:
 */
class WithdrawFragment : GenericFragment<FragmentWallteBinding>() {
    private val viewModel by viewModels<WithdrawViewModel>()
    private val methodAdapter = WithdrawMethodAdapter()
    private val amountAdapter = WithdrawAmountAdapter()
    private val cutInAdapter = WithdrawalCutInAdapter()
    private val userInfoMarqueeController = WithdrawUserInfoMarqueeController(this)
    private var withdrawalActionController: LayoutWithdrawalActionController? = null
    private val mWithdrawalLayoutTaskBinding: WithdrawalLayoutTaskBinding by lazy {
        WithdrawalLayoutTaskBinding.inflate(layoutInflater, binding.viewParent, false)
    }

    override fun bindView(inflater: LayoutInflater, container: ViewGroup?): FragmentWallteBinding {
        return FragmentWallteBinding.inflate(inflater, container, false)
    }

    override fun initUI() {
        viewModel.init()
        setupWithdrawMethods()
        binding.cutRv.adapter = cutInAdapter
        cutInAdapter.onCutClick = { item, holder ->
            (activity as? GenericActivity)?.let { act ->
                localEvent("ad_chance", params = hashMapOf("ad_pos_id" to WithdrawalCutInAdapter.CUT_IN_RV_AD_POSITION))
                AdUtils.showRvAd(act, adPositionName = WithdrawalCutInAdapter.CUT_IN_RV_AD_POSITION) { success ->
                    if (!success || !isAdded) return@showRvAd
                    viewLifecycleOwner.lifecycleScope.launch {
                        localEvent("cut_in")
                        val result = viewModel.applyCutInBoost(item.recordId)
                        if (result == null) {
                            Toast.makeText(context, getString(R.string.cut_in_boost_failed), Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        val oldP = (result.oldProgress * 100.0).toInt().coerceIn(10, 100)
                        val newP = (result.newProgress * 100.0).toInt().coerceIn(10, 100)
                        val plus = "+${result.displayedDeltaPercent}%"
                        holder.playBoostSequence(oldP, newP, plus) {
                            viewModel.refresh()
                        }
                    }
                }
            }
        }
        binding.tvAccount.setOnClickListener {
            DialogFragmentDisplayHelper.show(parentFragmentManager, MyAccountDialogFragment())
        }
        userInfoMarqueeController.setup(binding.layoutWithUserInfo)
        AnimViewHelper.applyPressBounceEffect(binding.tvWithdraw)
        setupImeInsetForScroll()
        registerViewModel()
        HelperRewardShow.curGetMoneyStr.observe(this) {
            binding.tvMoney.text = it.first
        }
    }

    private fun setupImeInsetForScroll() {
        val scrollView = binding.scrollWithdraw

        // 记录滚动容器的基础 bottom padding，避免重复叠加。
        val baseBottomPadding = scrollView.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(scrollView) { v, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottom = maxOf(baseBottomPadding, imeInsets.bottom)
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, bottom)
            insets
        }

        // 触发一次初始化，确保首次进入页面就能正确设置 padding。
        ViewCompat.requestApplyInsets(scrollView)
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

    override fun onDestroyView() {
        withdrawalActionController?.detach()
        withdrawalActionController = null
        userInfoMarqueeController.stop()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        localEvent("withdraw_page")
        refreshAndShowTaskDialog()
        methodAdapter.updateSelected()
        withdrawalActionController?.run {
            refreshMinTips()
            refreshMainWithdrawButton(binding.tvWithdraw)
        }
    }

    private fun registerViewModel() {
        viewModel.isShowMyAccount.observe(this) { it ->
            if (it) {
                binding.tvAccount.visibility = View.VISIBLE
            } else {
                binding.tvAccount.visibility = View.GONE
            }
        }
        viewModel.cutInItems.observe(this) { list ->
            cutInAdapter.submitList(list)
        }
        viewModel.curInfo.observe(this) { t ->
            mWithdrawalLayoutTaskBinding.apply {
                t?.let {
                    ivType.setImageResource(WithdrawAmountHelper.findWithdrawPaymentMethodById(WithdrawalActionHelper.withdrawalMethodId).iconSelected)
                    tvMoney.text = WithdrawAmountHelper.moneyFormatAddUnit(WithdrawalActionHelper.withdrawalValue)
                    tvTaskTitle.setText(t.first)
                    tvDes.setText(t.second)
                    bindTaskItem(taskItem1, t.third.getOrNull(0))
                    bindTaskItem(taskItem2, t.third.getOrNull(1))
                }
            }
        }
        viewModel.curStatus.observe(this) {
            when (it) {
                WithdrawalStatus.NORMAL -> {
                    binding.cutRv.visibility = View.GONE
                    binding.groupNormal.visibility = View.VISIBLE
                    binding.groupWithdrawal.visibility = View.GONE
                    binding.tvWithdraw.isEnabled = true
                    binding.tvWithdraw.setBackgroundResource(R.drawable.shape_bg_ye)
                    binding.viewParent.removeAllViews()
                    setupWithdrawAmounts()
                    binding.tvWithdraw.setOnClickListener {
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

                WithdrawalStatus.WithdrawalCut -> {
                    showWithdrawalActionFlow(showCutList = true)
                }

                WithdrawalStatus.Withdrawal1 -> {
                    showWithdrawalActionFlow(showCutList = false)
                }

                WithdrawalStatus.WithdrawalTask -> {
                    binding.cutRv.visibility = View.GONE
                    binding.apply {
                        binding.groupNormal.visibility = View.GONE
                        binding.groupWithdrawal.visibility = View.GONE
                        tvWithdraw.visibility = View.GONE
                        binding.viewParent.removeAllViews()
                    }
                    binding.viewParent.addView(mWithdrawalLayoutTaskBinding.root)
                }
            }
        }
    }

    private fun showWithdrawalActionFlow(showCutList: Boolean) {
        binding.cutRv.visibility = if (showCutList) View.VISIBLE else View.GONE
        binding.groupNormal.visibility = View.GONE
        binding.groupWithdrawal.visibility = View.VISIBLE
        binding.tvWithdraw.visibility = View.VISIBLE
        withdrawalActionController?.detach()
        withdrawalActionController = null
        binding.viewParent.removeAllViews()
        val actionBinding = LayoutWithdrawalActionBinding.inflate(layoutInflater, binding.viewParent, false)
        binding.viewParent.addView(actionBinding.root)
        withdrawalActionController = LayoutWithdrawalActionController(this, actionBinding).also {
            it.attach(binding.tvWithdraw)
            it.taskEvent = {
                refreshAndShowTaskDialog()
            }
        }
        binding.tvWithdraw.setOnClickListener {
            withdrawalActionController?.let { c ->
                if (!binding.tvWithdraw.isEnabled) return@setOnClickListener
                runCatching {
                    localEvent("withdrawal_c")
                    WithdrawalActionHelper.withdrawalMethodId = methodAdapter.getSelectedMethod()?.id ?: ""
                    c.handleMainWithdrawClick(parentFragmentManager)
                }
            }
        }
    }

    private fun mapTaskType(progress: Int): String {
        return when (progress) {
            TASK1_STEP -> "task1"
            TASK2_STEP -> "task2"
            TASK3_STEP -> "task3"
            else -> "task1"
        }
    }

    private fun refreshAndShowTaskDialog() {
        viewModel.refresh({ progress ->
            activity?.let {
                if (it.isFinishing || isAdded.not()) return@refresh
                val taskInfo = viewModel.curInfo.value
                when (progress) {
                    TASK3_STEP -> {
                        DialogFragmentDisplayHelper.show(it.supportFragmentManager, FlipCardDialogFragment().apply {
                            dismissEvent = {
                                if (it.isFinishing.not() && isAdded && isResume) {
                                    taskInfo?.let { info ->
                                        DialogFragmentDisplayHelper.show(
                                            it.supportFragmentManager,
                                            WithdrawalTaskFragment.newInstance(
                                                title = getString(info.first),
                                                desc = getString(info.second),
                                                tasks = info.third,
                                                type = mapTaskType(progress),
                                            ),
                                        )
                                    }
                                }
                            }
                        })
                    }

                    100 -> {
                        DialogFragmentDisplayHelper.show(it.supportFragmentManager,
                            WithdrawApplyTransitionDialogFragment().apply {
                                dismissEvent = {
                                    if (it.isFinishing.not() && isAdded && isResume) {
                                        viewModel.refresh()
                                    }
                                }
                            })
                    }

                    else -> {
                        taskInfo?.let { info ->
                            DialogFragmentDisplayHelper.show(
                                it.supportFragmentManager,
                                WithdrawalTaskFragment.newInstance(
                                    title = getString(info.first),
                                    desc = getString(info.second),
                                    tasks = info.third,
                                    type = mapTaskType(progress),
                                ),
                            )
                        }
                    }
                }

            }
        })
    }

    private fun bindTaskItem(binding: ItemWithdrawalTaskBinding, item: WithdrawalTaskItem?) {
        if (item == null) {
            binding.root.visibility = View.GONE
            return
        }
        binding.root.visibility = View.VISIBLE
        binding.tvTask.text = item.text
        binding.tvProgress.text = item.progressText
        binding.tvProgress.visibility = if (item.isCompleted) View.GONE else View.VISIBLE
        binding.ivDone.visibility = if (item.isCompleted) View.VISIBLE else View.GONE
        binding.tvTask.setTextColor(
            requireContext().getColor(
                if (item.isCompleted) R.color.color_46d else android.R.color.white,
            ),
        )
    }
}