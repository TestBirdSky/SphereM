package com.sphere.shortvideos.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sphere.shortvideos.R
import com.sphere.shortvideos.databinding.ItemWithdrawalBinding

class WithdrawMethodAdapter(

) : RecyclerView.Adapter<WithdrawMethodAdapter.MethodViewHolder>() {

    private val selected = listOf<Int>(R.drawable.ic_pix_w, R.drawable.ic_ovo_w, R.drawable.ic_paypal_white,R.drawable.ic_daa_w,R.drawable.ic_pagbank_w)
    private val icons = listOf(R.drawable.ic_pix_b, R.drawable.ic_ovo_b, R.drawable.ic_paypal_black,R.drawable.ic_daa_b,R.drawable.ic_pagbank_b)
    private var selectedIndex = 0
    var onItemClick: ((index: Int) -> Unit)? = null


    fun setSelected(index: Int) {
        if (index == selectedIndex) return
        val old = selectedIndex
        selectedIndex = index
        if (old in icons.indices) notifyItemChanged(old)
        if (index in icons.indices) notifyItemChanged(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MethodViewHolder {
        val binding = ItemWithdrawalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MethodViewHolder(binding)
    }

    override fun getItemCount(): Int = icons.size

    override fun onBindViewHolder(holder: MethodViewHolder, position: Int) {

        holder.bind(position)
    }

    inner class MethodViewHolder(private val binding: ItemWithdrawalBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(position:Int) {
            val iconRes = if (position == selectedIndex) selected[position] else icons[position]
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
