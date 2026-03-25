package com.sphere.shortvideos.vm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chartboost.sdk.impl.fa
import com.sphere.shortvideos.R
import com.sphere.shortvideos.adapter.WithdrawalCutInItem
import com.sphere.shortvideos.dialogs.withdraw.WithdrawalTaskItem
import com.sphere.shortvideos.helper.WithdrawAmountHelper
import com.sphere.shortvideos.helper.task.TaskHelper
import com.sphere.shortvideos.helper.withdraw.TASK1_STEP
import com.sphere.shortvideos.helper.withdraw.TASK2_STEP
import com.sphere.shortvideos.helper.withdraw.TASK3_STEP
import com.sphere.shortvideos.helper.withdraw.WithdrawalActionHelper
import com.sphere.shortvideos.helper.withdraw.WithdrawalActionHelper.curTaskProgressStatus
import com.sphere.shortvideos.helper.withdraw.WithdrawalActionHelper.isShowWithApplyDialog
import com.sphere.shortvideos.helper.withdraw.WithdrawalStatus
import com.sphere.shortvideos.helper.withdraw.db.CutInBoostResult
import com.sphere.shortvideos.helper.withdraw.db.WithdrawalRecordEntity
import com.sphere.shortvideos.helper.withdraw.db.WithdrawalRecordStore
import com.sphere.shortvideos.logError
import com.sphere.shortvideos.mApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Date：2026/3/25
 * Describe:
 */
class WithdrawViewModel : ViewModel() {
    var curStatus = MutableLiveData(WithdrawalStatus.NORMAL)

    var isShowMyAccount = MutableLiveData<Boolean>(false)

    var curTaskStep = MutableLiveData(curTaskProgressStatus)
    var curInfo = MutableLiveData<Triple<Int, Int, List<WithdrawalTaskItem>>?>()
    val cutInItems = MutableLiveData<List<WithdrawalCutInItem>>(emptyList())


    fun init() {
        viewModelScope.launch(Dispatchers.IO) {
            val records = WithdrawalRecordStore.getAllRecordsOrdered()
            if (records.isNotEmpty()) {
                isShowMyAccount.postValue(true)
            }
        }
    }

    /**
     * 刷新提现态与「插队」列表。
     *
     * - 提现能力关闭 → [WithdrawalStatus.NORMAL]
     * - 仍可提现且库中存在未完成提现记录 → [WithdrawalStatus.WithdrawalCut]（展示 [cutInItems]）
     * - 仍可提现且无记录 → [WithdrawalStatus.Withdrawal1]
     */
    fun refresh(showDialogEvent: (Int) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.Main) {
            val openWithdraw = WithdrawalActionHelper.getConfig().isOpenWithdraw()
            if (!openWithdraw) {
                cutInItems.value = emptyList()
                curStatus.value = WithdrawalStatus.NORMAL
                isShowMyAccount.value = false
                return@launch
            }
            isShowMyAccount.postValue(true)
            if (curTaskProgressStatus > 0) {
                refreshTask(showDialogEvent)
                return@launch
            }
            withContext(Dispatchers.IO) {
                val records = WithdrawalRecordStore.getAllRecordsOrdered()
                val uiItems = records.map { it.toCutInItem() }
                val status = if (records.isNotEmpty()) {
                    WithdrawalStatus.WithdrawalCut
                } else {
                    WithdrawalStatus.Withdrawal1
                }
                cutInItems.postValue(uiItems)
                curStatus.postValue(status)
            }

        }
    }

    private fun refreshTask(block: (Int) -> Unit) {
        logError("refreshTask-->$curTaskProgressStatus")
        if ((curTaskStep.value ?: 0) < 100) {
            var info = fetchInfoByStep()
            val isCompleted = info?.third?.all { it.isCompleted } ?: false
            if (isCompleted && curTaskProgressStatus < 100) {
                curTaskProgressStatus += 30
                info = fetchInfoByStep()
            }
            curStatus.value = WithdrawalStatus.WithdrawalTask

            val oldStep = curTaskStep.value ?: 0
            val newStep = curTaskProgressStatus
            curTaskStep.value = newStep
            curInfo.value = info
            if (newStep != oldStep) {
                block(newStep)
            } else if (newStep == 100 && isShowWithApplyDialog.not()) {
                block(newStep)
            }
        }
    }

    suspend fun applyCutInBoost(recordId: Long): CutInBoostResult? {
        return withContext(Dispatchers.IO) {
            WithdrawalRecordStore.tryApplyCutInBoost(recordId)
        }
    }

    private fun WithdrawalRecordEntity.toCutInItem(): WithdrawalCutInItem {
        val method = WithdrawAmountHelper.findWithdrawPaymentMethodById(withdrawalMethodId)
        val amountText = WithdrawAmountHelper.moneyFormatAddUnit(withdrawalAmount)
        val percent = (progress * 100.0).toInt().coerceIn(10, 100)
        val baseline = 0.1
        val delta = ((progress - baseline) * 100.0).toInt()
        val addText = if (delta > 0) "+${delta}%" else null
        val canBoost = progress < 1.0 - 1e-9
        return WithdrawalCutInItem(
            recordId = id,
            methodIconRes = method.iconSelected,
            amountText = amountText,
            progressPercent = percent,
            addPercentDisplay = addText,
            canBoostToday = canBoost,
        )
    }

    fun fetchInfoByStep(): Triple<Int, Int, List<WithdrawalTaskItem>>? {
        val signDoneToday = isSignDoneToday()
        return when (curTaskProgressStatus) {
            TASK1_STEP -> {
                val target = WithdrawalActionHelper.getConfig().withdrawalTask?.task1?.drama?.coerceAtLeast(1) ?: 3
                val cur = WithdrawalActionHelper.getPrimaryProgressByCurStep().coerceAtMost(target)
                Triple(
                    R.string.security_verification,
                    R.string.task_des1,
                    listOf(
                        buildSignItem(signDoneToday),
                        buildItem(mApp.getString(R.string.watch_min_drama, target), cur, target),
                    ),
                )
            }

            TASK2_STEP -> {
                val target = WithdrawalActionHelper.getConfig().withdrawalTask?.task2?.bubble?.coerceAtLeast(1) ?: 2
                val cur = WithdrawalActionHelper.getPrimaryProgressByCurStep().coerceAtMost(target)
                Triple(
                    R.string.identity_confirmation,
                    R.string.task_des2,
                    listOf(
                        buildSignItem(signDoneToday),
                        buildItem(mApp.getString(R.string.claim_bubbles, target), cur, target),
                    ),
                )
            }

            TASK3_STEP -> {
                val target = WithdrawalActionHelper.getConfig().withdrawalTask?.task3?.ad?.coerceAtLeast(1) ?: 5
                val cur = WithdrawalActionHelper.getPrimaryProgressByCurStep().coerceAtMost(target)
                Triple(
                    R.string.compliance_check,
                    R.string.task_des3,
                    listOf(
                        buildSignItem(signDoneToday),
                        buildItem(mApp.getString(R.string.watch_ads, target), cur, target),
                    ),
                )
            }

            else -> null
        }
    }

    private fun buildItem(text: String, current: Int, target: Int): WithdrawalTaskItem {
        val done = current >= target
        return WithdrawalTaskItem(
            text = text,
            isCompleted = done,
            progressText = if (done) "" else "$current/$target",
        )
    }

    private fun buildSignItem(isDone: Boolean): WithdrawalTaskItem {
        return WithdrawalTaskItem(
            text = mApp.getString(R.string.daily_check_in),
            isCompleted = isDone,
            progressText = if (isDone) "" else "0/2",
        )
    }

    private fun isSignDoneToday(): Boolean {
        val states = TaskHelper.fetchSignInStates()
        return states.none { it.status == TaskHelper.SignInStatus.CLAIMABLE }
    }
}
