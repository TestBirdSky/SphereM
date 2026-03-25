package com.sphere.shortvideos.adapter

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sphere.shortvideos.databinding.ItemLayoutCutInBinding

data class WithdrawalCutInItem(
    val recordId: Long,
    val methodIconRes: Int,
    val amountText: String,
    /** [10, 100]，与 [WithdrawalRecordEntity.progress] 0.1～1.0 对应 */
    val progressPercent: Int,
    /** 相对初始 10% 的增量，如 "+20%"；为 null 时隐藏 */
    val addPercentDisplay: String?,
    /** 只要未满 100% 就可看广告继续加速 */
    val canBoostToday: Boolean,
)

/**
 * 「插队」提现进度卡片列表，布局 [R.layout.item_layout_cut_in]。
 */
class WithdrawalCutInAdapter : RecyclerView.Adapter<WithdrawalCutInAdapter.CutInViewHolder>() {

    /** 点击「Cut In」：由页面内判断激励、领进度、再调 [CutInViewHolder.playBoostSequence] */
    var onCutClick: ((item: WithdrawalCutInItem, holder: CutInViewHolder) -> Unit)? = null

    private var items: List<WithdrawalCutInItem> = emptyList()

    fun submitList(list: List<WithdrawalCutInItem>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CutInViewHolder {
        val binding = ItemLayoutCutInBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CutInViewHolder(binding) { item, holder ->
            onCutClick?.invoke(item, holder)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: CutInViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class CutInViewHolder(
        private val binding: ItemLayoutCutInBinding,
        private val cutClick: (WithdrawalCutInItem, CutInViewHolder) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        private var bindGeneration = 0L
        private var pendingEndRunnable: Runnable? = null

        fun bind(item: WithdrawalCutInItem) {
            bindGeneration++
            pendingEndRunnable?.let { binding.root.removeCallbacks(it) }
            pendingEndRunnable = null

            binding.ivWithMethod.setImageResource(item.methodIconRes)
            binding.tvMoney.text = item.amountText
            val percent = item.progressPercent.coerceIn(0, 100)
            binding.progressView.progress = percent
            binding.tvCurProgress.text = "${percent}%"
            // `tvAddProgress` 仅用于“看广告后短暂展示 +X%”的动效，避免被列表复用导致常驻显示。
            binding.tvAddProgress.animate().cancel()
            binding.tvAddProgress.isVisible = false
            binding.tvAddProgress.text = ""
            binding.tvAddProgress.alpha = 1f
            binding.tvAddProgress.scaleX = 1f
            binding.tvAddProgress.scaleY = 1f

            val canTap = item.progressPercent < 100 && item.canBoostToday
            binding.tvCut.isEnabled = canTap
            binding.tvCut.alpha = if (canTap) 1f else 0.45f
            binding.tvCut.setOnClickListener {
                if (!canTap) return@setOnClickListener
                cutClick(item, this@CutInViewHolder)
            }
        }

        /**
         * T0：调用此方法（广告已发奖且服务端/本地进度已落库）。
         * T0～T0+1s：金额区域缩放脉冲（进度条不动）。
         * T0+1s：进度条动画涨至 [newPercent]，并展示 [plusText]。
         * T0+3s：隐藏 「+」 文案（与涨进度同时开始再过 2s）。
         */
        fun playBoostSequence(
            oldPercent: Int,
            newPercent: Int,
            plusText: String,
            onSequenceEnded: () -> Unit,
        ) {
            val gen = bindGeneration

            fun isStale(): Boolean = gen != bindGeneration

            pendingEndRunnable?.let { binding.root.removeCallbacks(it) }
            pendingEndRunnable = null

            binding.tvCut.isEnabled = false
            binding.tvAddProgress.animate().cancel()
            binding.tvAddProgress.isVisible = false
            binding.tvAddProgress.text = ""
            binding.tvAddProgress.alpha = 1f
            binding.tvAddProgress.scaleX = 1f
            binding.tvAddProgress.scaleY = 1f

            // 第一段：1s「增加」动效（进度数字与条暂不跳）
            val intro = ObjectAnimator.ofPropertyValuesHolder(
                binding.tvMoney,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.18f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.18f, 1f),
            ).apply {
                duration = 1000L
                interpolator = AccelerateDecelerateInterpolator()
            }
            intro.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.tvMoney.scaleX = 1f
                    binding.tvMoney.scaleY = 1f
                }
            })
            intro.start()

            binding.root.postDelayed({
                if (isStale()) {
                    onSequenceEnded()
                    return@postDelayed
                }

                binding.tvAddProgress.text = plusText
                binding.tvAddProgress.isVisible = true
                binding.tvAddProgress.alpha = 0f
                binding.tvAddProgress.scaleX = 0.6f
                binding.tvAddProgress.scaleY = 0.6f
                binding.tvAddProgress.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(280L)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()

                val clampedOld = oldPercent.coerceIn(0, 100)
                val clampedNew = newPercent.coerceIn(0, 100)
                val progressAnim = ValueAnimator.ofInt(clampedOld, clampedNew).apply {
                    duration = 850L
                    interpolator = AccelerateDecelerateInterpolator()
                    addUpdateListener { va ->
                        val v = va.animatedValue as Int
                        binding.progressView.progress = v
                        binding.tvCurProgress.text = "$v%"
                    }
                }
                progressAnim.start()

                val hideRunnable = Runnable {
                    if (isStale()) {
                        onSequenceEnded()
                        return@Runnable
                    }
                    binding.tvAddProgress.animate()
                        .alpha(0f)
                        .setDuration(220L)
                        .withEndAction {
                            binding.tvAddProgress.isVisible = false
                            binding.tvAddProgress.alpha = 1f
                            binding.tvAddProgress.text = ""
                            onSequenceEnded()
                        }
                        .start()
                }
                pendingEndRunnable = hideRunnable
                binding.root.postDelayed(hideRunnable, 2000L)
            }, 1000L)
        }
    }

    companion object {
        /** 与提现流程激励位一致，需与广告配置中的 position 名称对齐 */
        const val CUT_IN_RV_AD_POSITION = "dlmsf_withdraw_skip_rv"
    }
}
