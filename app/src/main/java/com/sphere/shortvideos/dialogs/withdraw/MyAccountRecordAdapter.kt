package com.sphere.shortvideos.dialogs.withdraw

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sphere.shortvideos.databinding.ItemMyAccountRecordBinding

data class MyAccountRecordRow(
    val dateText: String,
    val amountText: String,
    val statusText: String,
    val statusColor: Int,
)

class MyAccountRecordAdapter : RecyclerView.Adapter<MyAccountRecordAdapter.RowVH>() {
    private var items: List<MyAccountRecordRow> = emptyList()

    fun submitList(list: List<MyAccountRecordRow>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowVH {
        val binding = ItemMyAccountRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RowVH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RowVH, position: Int) {
        holder.bind(items[position])
    }

    class RowVH(private val binding: ItemMyAccountRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MyAccountRecordRow) {
            binding.tvDate.text = item.dateText
            binding.tvAmount.text = item.amountText
            binding.tvStatus.text = item.statusText
            binding.tvStatus.setTextColor(item.statusColor)
        }
    }
}

