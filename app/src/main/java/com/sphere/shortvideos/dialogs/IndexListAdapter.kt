package com.sphere.shortvideos.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.sphere.shortvideos.R
import com.sphere.shortvideos.databinding.ItemEpisodeIndexBinding

data class IndexTabData(
    var startIndex: Int,
    var endIndex: Int
)

class IndexListAdapter(
    private val context: Context,
    private val indexTabData: IndexTabData,
    private val currentIndex: Int,
    private val onItemClickCallback: (chooseIndex: Int) -> Unit
) : RecyclerView.Adapter<IndexListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemEpisodeIndexBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val index = indexTabData.startIndex + holder.layoutPosition
        holder.viewBinding.run {
            root.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(context, if (currentIndex == index) R.color.color_primary else R.color.color_dark_gery))
            itemText.text = "$index"
        }
        holder.itemView.setOnClickListener {
            onItemClickCallback.invoke(index)
        }
    }

    override fun getItemCount(): Int = indexTabData.endIndex - indexTabData.startIndex + 1


    inner class ViewHolder(val viewBinding: ItemEpisodeIndexBinding) : RecyclerView.ViewHolder(viewBinding.root)
}