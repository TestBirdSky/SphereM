package com.sphere.shortvideos.fragment.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bytedance.sdk.shortplay.api.ShortPlay
import com.sphere.shortvideos.database.DramaCollectEntity
import com.sphere.shortvideos.databinding.ItemHistroyBinding
import com.sphere.shortvideos.dp2Px
import com.sphere.shortvideos.fromJson

class CollectionAdapter(private val context: Context, private val onItemClicked: (DramaCollectEntity) -> Unit) :
    RecyclerView.Adapter<CollectionAdapter.ItemCollectViewHolder>() {

    private val datas = mutableListOf<DramaCollectEntity>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(data: List<DramaCollectEntity>) {
        datas.clear()
        datas.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = run {
        ItemCollectViewHolder(ItemHistroyBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ItemCollectViewHolder, position: Int) {
        val item = datas[holder.layoutPosition]
        holder.viewBinding.apply {
            if (item.isPangle) {
                val itemShortPlay = item.dataJson.fromJson<ShortPlay>()
                Glide.with(context).load(itemShortPlay.coverImage)
                    .transform(CenterCrop(), RoundedCorners(context.dp2Px(10))).into(imageCover)
                textName.text = itemShortPlay.title ?: ""
                textProgress.text = ""
                textEp.text = "Eps.${itemShortPlay.total}"
            } else {
                imageCover.setImageResource(0)
                textName.text = ""
                textProgress.text = ""
                textEp.text = ""
            }
        }
        holder.itemView.setOnClickListener {
            onItemClicked(item)
        }
    }

    override fun getItemCount(): Int = datas.size

    inner class ItemCollectViewHolder(val viewBinding: ItemHistroyBinding) : RecyclerView.ViewHolder(viewBinding.root)

}