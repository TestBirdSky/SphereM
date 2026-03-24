package com.sphere.shortvideos.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sphere.shortvideos.databinding.ItemWithdrawalBinding
import com.sphere.shortvideos.helper.WithdrawAmountHelper

/**
 * 提现方式横向列表，按地区显示对应图标（巴西/美国/印尼）
 */
class WithdrawMethodAdapter(
    private var methods: List<WithdrawAmountHelper.WithdrawPaymentMethod> = emptyList()
) : RecyclerView.Adapter<WithdrawMethodAdapter.MethodViewHolder>() {

    private var selectedIndex = 0
    var onItemClick: ((index: Int) -> Unit)? = null

    fun setMethods(list: List<WithdrawAmountHelper.WithdrawPaymentMethod>) {
        methods = list
        selectedIndex = 0
        notifyDataSetChanged()
    }

    fun setSelected(index: Int) {
        if (index == selectedIndex) return
        val old = selectedIndex
        selectedIndex = index
        if (old in methods.indices) notifyItemChanged(old)
        if (index in methods.indices) notifyItemChanged(index)
    }

    /** 当前选中的提现方式（与钱包页横向列表一致） */
    fun getSelectedMethod(): WithdrawAmountHelper.WithdrawPaymentMethod? = methods.getOrNull(selectedIndex)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MethodViewHolder {
        val binding = ItemWithdrawalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MethodViewHolder(binding)
    }

    override fun getItemCount(): Int = methods.size

    override fun onBindViewHolder(holder: MethodViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class MethodViewHolder(private val binding: ItemWithdrawalBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            val method = methods.getOrNull(position) ?: return
            val iconRes = if (position == selectedIndex) method.iconSelected else method.iconNormal
            binding.ivWithdrawalType.setImageResource(iconRes)
            binding.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                setSelected(pos)
                onItemClick?.invoke(pos)
            }
        }
    }
}
