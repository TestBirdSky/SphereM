package com.sphere.shortvideos.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.sphere.shortvideos.R
import com.sphere.shortvideos.view.AnimViewHelper
import com.sphere.shortvideos.databinding.Item7dayClickBinding

class SevenDayRewardAdapter : RecyclerView.Adapter<SevenDayRewardAdapter.RewardViewHolder>() {

    enum class SignInStatus {
        CLAIMED, CLAIMABLE, UNCLAIMED
    }

    data class SevenDayRewardItem(val day: Int, val rewardText: String, val status: SignInStatus)

    private val items = mutableListOf<SevenDayRewardItem>()
    var onItemClick: ((SevenDayRewardItem, ImageView) -> Unit)? = null

    fun submitList(list: List<SevenDayRewardItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardViewHolder {
        val binding = Item7dayClickBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RewardViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RewardViewHolder, position: Int) {
        holder.bind(items[position], onItemClick)
    }

    class RewardViewHolder(private val binding: Item7dayClickBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SevenDayRewardItem, onItemClick: ((SevenDayRewardItem, ImageView) -> Unit)?) {
            binding.tvDayTitle.text = "Day${item.day}"
            binding.tvDayDesc.text = item.rewardText
            val isDay7 = item.day == 7
            val iconRes = if (isDay7) {
                R.drawable.ic_last_day
            } else {
                R.drawable.ic_normal_day
            }
            binding.ivDay.setImageResource(iconRes)
            val isClaimable = item.status == SignInStatus.CLAIMABLE
            var textColor = binding.root.context.getColor(R.color.white)
            when (item.status) {
                SignInStatus.CLAIMED -> {
                    binding.ivChecked.visibility = View.VISIBLE
                    binding.layoutDay.setBackgroundResource(R.drawable.shape_day_receiver)
                    textColor = binding.root.context.getColor(R.color.color_9b)
                    if (isDay7) {
                        binding.ivDay.alpha = 0.3f
                        binding.ivDay.setImageResource(R.drawable.ic_last_day)
                    } else {
                        binding.ivDay.setImageResource(R.drawable.ic_finish_7day)
                    }
                }

                SignInStatus.CLAIMABLE -> {
                    binding.ivChecked.visibility = View.GONE
                    binding.layoutDay.setBackgroundResource(R.drawable.shape_can_receiver)
                }

                else -> {
                    binding.layoutDay.setBackgroundResource(R.drawable.shape_op)
                    binding.ivChecked.visibility = View.GONE
                }
            }

            binding.tvDayTitle.setTextColor(textColor)
            binding.tvDayDesc.setTextColor(textColor)
            AnimViewHelper.playClaimablePulseAnim(binding.layoutDay, isClaimable, 0.82f, 1.03f)
            binding.layoutDay.setOnClickListener {
                if (isClaimable) {
                    onItemClick?.invoke(item, binding.ivDay)
                }
            }
        }
    }
}
