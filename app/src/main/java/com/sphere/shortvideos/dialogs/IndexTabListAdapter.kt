package com.sphere.shortvideos.dialogs

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class IndexTabListAdapter(
    private val context: Context,
    private val indexTabDatas: MutableList<IndexTabData>,
    private val currentIndex: Int,
    private val onItemClickCallback: (chooseIndex: Int) -> Unit
) : RecyclerView.Adapter<IndexTabListAdapter.IndexTabVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IndexTabVH {
        val recyclerView = RecyclerView(parent.context)
        recyclerView.setLayoutManager(GridLayoutManager(parent.context, 5))
        recyclerView.setLayoutParams(ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        recyclerView.itemAnimator = null
        return IndexTabVH(recyclerView, onItemClickCallback)
    }

    override fun onBindViewHolder(holder: IndexTabVH, position: Int) {
        holder.bindData(indexTabDatas[holder.layoutPosition], currentIndex)
    }

    override fun getItemCount(): Int = indexTabDatas.size

    inner class IndexTabVH(private val recyclerView: RecyclerView, private val onItemClickCallback: (chooseIndex: Int) -> Unit) : RecyclerView.ViewHolder(recyclerView) {

        fun bindData(tabData: IndexTabData, currentPlayingIndex: Int) {
            recyclerView.setAdapter(IndexListAdapter(context, tabData, currentPlayingIndex, onItemClickCallback))
        }
    }
}



