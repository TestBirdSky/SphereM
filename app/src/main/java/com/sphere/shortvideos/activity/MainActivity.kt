package com.sphere.shortvideos.activity

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.sphere.shortvideos.R
import com.sphere.shortvideos.baseui.GenericBindActivity
import com.sphere.shortvideos.databinding.ActivityMainBinding
import com.sphere.shortvideos.fragment.HomeFragment
import com.sphere.shortvideos.fragment.ProfileFragment
import com.sphere.shortvideos.fragment.TaskFragment
import com.sphere.shortvideos.fragment.VideoStreamFragment
import com.sphere.shortvideos.fragment.WalletFragment
import com.sphere.shortvideos.vm.MainViewModel

class MainActivity : GenericBindActivity<ActivityMainBinding>() {

    private val viewModel by viewModels<MainViewModel>()
    private val walletIndex = 2 // 钱包是第3个（从0开始）

    override val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun initUI() {
        setupBottomNav()
        viewModel.collectHistoryRoom()
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnApplyWindowInsetsListener(null)

        // 禁用自动 tint，手动控制颜色
        binding.bottomNav.itemIconTintList = null
        binding.bottomNav.itemTextColor = null
        val fragments =
            listOf(HomeFragment(), VideoStreamFragment(), WalletFragment(), TaskFragment(), ProfileFragment())
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.offscreenPageLimit = fragments.size
        binding.viewPager.adapter = object : FragmentStateAdapter(supportFragmentManager, lifecycle) {
            override fun getItemCount(): Int = fragments.size
            override fun createFragment(position: Int): Fragment = fragments[position]
        }
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.bottomNav.menu[position].isChecked = true
                binding.bottomNav.menu.findItem(R.id.tab_wallet).icon =
                    ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_nav_bill)
            }
        })
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.tab_home -> {
                    binding.bottomNav.menu.findItem(R.id.tab_home).icon =
                        ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_nav_home)
                    binding.viewPager.setCurrentItem(0, false)
                }

                R.id.tab_video -> {
                    binding.bottomNav.menu.findItem(R.id.tab_video).icon =
                        ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_nav_video_selected)
                    binding.viewPager.setCurrentItem(1, false)
                }

                R.id.tab_wallet -> {
                    binding.viewPager.setCurrentItem(2, false)

                }

                R.id.tab_task -> {
                    binding.bottomNav.menu.findItem(R.id.tab_task).icon =
                        ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_nav_mi_selected)
                    binding.viewPager.setCurrentItem(3, false)
                }

                R.id.tab_user -> {
                    binding.bottomNav.menu.findItem(R.id.tab_user).icon =
                        ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_nav_user_selected)
                    binding.viewPager.setCurrentItem(4, false)
                }
            }
            true
        }
        binding.viewPager.setCurrentItem(1, false)
    }

}