package com.sphere.shortvideos.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sphere.shortvideos.R
import com.sphere.shortvideos.databinding.ItemWithAmountBinding
import com.sphere.shortvideos.helper.WithdrawAmountHelper

class WithdrawAmountAdapter : RecyclerView.Adapter<WithdrawAmountAdapter.AmountViewHolder>() {

    private val items = mutableListOf<Double>()
    private var selectedIndex = 0
    var onItemClick: ((index: Int) -> Unit)? = null

    fun fetchWithdrawMoney(): Double {
        return items[selectedIndex]
    }

    fun submitList(list: List<Double>) {
        items.clear()
        items.addAll(list)
        selectedIndex = 0
        notifyDataSetChanged()
    }

    fun setSelected(index: Int) {
        if (index == selectedIndex) return
        val old = selectedIndex
        selectedIndex = index
        if (old in items.indices) notifyItemChanged(old)
        if (index in items.indices) notifyItemChanged(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AmountViewHolder {
        val binding = ItemWithAmountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AmountViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: AmountViewHolder, position: Int) {
        holder.bind(items[position], position == selectedIndex)
    }

    inner class AmountViewHolder(private val binding: ItemWithAmountBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(amount: Double, isSelected: Boolean) {
            binding.tvInfo.text = WithdrawAmountHelper.moneyFormatAddUnitWithNoSpace(amount)
            binding.tvInfo.setBackgroundResource(if (isSelected) R.drawable.shape_4e3_r7 else R.drawable.shape_r7_2d1d45)
            binding.tvInfo.setTextColor(if (isSelected) binding.root.context.getColor(R.color.white) else binding.root.context.getColor(
                R.color.color_ca))
            binding.root.setOnClickListener {
                setSelected(bindingAdapterPosition)
                onItemClick?.invoke(bindingAdapterPosition)
            }
        }
    }
}
