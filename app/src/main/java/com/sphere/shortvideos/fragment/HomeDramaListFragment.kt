package com.sphere.shortvideos.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bytedance.sdk.shortplay.api.ShortPlay
import com.scwang.smart.refresh.footer.BallPulseFooter
import com.scwang.smart.refresh.header.MaterialHeader
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener
import com.sphere.shortvideos.GlobalConstants
import com.sphere.shortvideos.R
import com.sphere.shortvideos.activity.PangleDramaPlayActivity
import com.sphere.shortvideos.baseui.GenericFragment
import com.sphere.shortvideos.databinding.FragmentHomeDramaListBinding
import com.sphere.shortvideos.databinding.ItemDramaInfoBinding
import com.sphere.shortvideos.dp2Px
import com.sphere.shortvideos.helper.ad.LaunchPosition
import com.sphere.shortvideos.helper.localEvent
import com.sphere.shortvideos.nextView
import com.sphere.shortvideos.showToast
import com.sphere.shortvideos.vm.DramaListViewModel

class HomeDramaListFragment : GenericFragment<FragmentHomeDramaListBinding>() {

    private var categoryId: Long = 0L
    private val viewModel by viewModels<DramaListViewModel>()
    private lateinit var mAdapter: DramaInfoListAdapter
    private var initialized: Boolean = false

    companion object {
        fun newInstance(categoryId: Long): HomeDramaListFragment {
            val args = Bundle().apply {
                putLong("CATEGORY_ID", categoryId)
            }
            return HomeDramaListFragment().apply { arguments = args }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.run {
            categoryId = getLong("CATEGORY_ID", 0)
        }
    }

    override fun bindView(inflater: LayoutInflater, container: ViewGroup?) = run {
        FragmentHomeDramaListBinding.inflate(inflater, container, false)
    }

    override fun initUI() {
        viewModel.onErrorLiveData.observe(this) {
            if (it) {
                initialized = false
                binding.emptyView.root.isVisible = true
                binding.refreshLayout.finishRefresh()
            } else {
                binding.refreshLayout.finishLoadMore()
            }
        }
        viewModel.refreshLiveData.observe(this) { result ->
            binding.refreshLayout.finishRefresh()
            if (result.isNotEmpty()) {
                binding.emptyView.root.isVisible = false
                mAdapter.submitData(result)
            } else {
                initialized = false
                binding.emptyView.root.isVisible = true
            }
        }
        viewModel.addLiveData.observe(this) { result ->
            binding.refreshLayout.finishLoadMore()
            mAdapter.addData(result)
        }
        binding.refreshLayout.setRefreshHeader(MaterialHeader(requireContext()).apply {
            setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.color_primary))
        })
        binding.refreshLayout.setRefreshFooter(BallPulseFooter(requireContext()).apply {
            setAnimatingColor(ContextCompat.getColor(requireContext(), R.color.color_primary))
        })
        binding.refreshLayout.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
                if (viewModel.hasMoreData) viewModel.loadData(categoryId) else binding.refreshLayout.finishRefresh(1000)
            }

            override fun onLoadMore(refreshLayout: RefreshLayout) {
                if (viewModel.hasMoreData) viewModel.loadMore(categoryId) else {
                    binding.refreshLayout.finishLoadMore()
                    context?.showToast(getString(R.string.no_more_data))
                }
            }
        })
        mAdapter = DramaInfoListAdapter(requireContext()) {
            localEvent("list_drama_c", hashMapOf("name" to it.title))
            requireContext().nextView<PangleDramaPlayActivity> {
                putExtra(GlobalConstants.EXTRA_KEY_SHORT_PLAY, it)
            }
        }
        binding.recyclerView.adapter = mAdapter
        if (-2L == categoryId) autoRefresh()
    }

    private fun autoRefresh() {
        if (!initialized) {
            initialized = true
            binding.refreshLayout.autoRefresh()
        }
    }

    override fun onResume() {
        super.onResume()
        autoRefresh()
    }

    inner class DramaInfoItemViewHolder(val viewbinding: ItemDramaInfoBinding) : RecyclerView.ViewHolder(viewbinding.root)

    inner class DramaInfoListAdapter(private val context: Context, private val onItemClicked: (ShortPlay) -> Unit) : RecyclerView.Adapter<DramaInfoItemViewHolder>() {

        private val data: MutableList<ShortPlay> = mutableListOf()

        @SuppressLint("NotifyDataSetChanged")
        fun submitData(list: MutableList<ShortPlay>) {
            data.clear()
            data.addAll(list)
            notifyDataSetChanged()
        }

        fun addData(list: MutableList<ShortPlay>) {
            val oldSize = data.size
            data.addAll(list)
            notifyItemRangeInserted(oldSize, data.size)
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ) = run {
            DramaInfoItemViewHolder(ItemDramaInfoBinding.inflate(LayoutInflater.from(context), parent, false))
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: DramaInfoItemViewHolder, position: Int) {
            val item = data[holder.layoutPosition]
            holder.viewbinding.apply {
                Glide.with(context).load(item.coverImage)
                    .transform(CenterCrop(), RoundedCorners(context.dp2Px(10))).into(imageCover)
                textEps.text = "Eps.${item.total}"
                textName.text = item.title ?: ""
                val tag = item.tags.lastOrNull()
                if (1L == tag?.id || -2L == categoryId) {
                    textTag.text = context.getString(R.string.category_hot)
                    textTag.isVisible = true
                } else if (2L == tag?.id || -1L == categoryId) {
                    textTag.text = context.getString(R.string.category_new)
                    textTag.isVisible = true
                } else {
                    textTag.isVisible = false
                }
            }
            holder.itemView.setOnClickListener {
                onItemClicked(item)
            }
        }

        override fun getItemCount(): Int = data.size

    }


}