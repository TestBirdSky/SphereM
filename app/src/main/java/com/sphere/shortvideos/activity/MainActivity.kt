package com.sphere.shortvideos.activity

import androidx.activity.viewModels
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.sphere.shortvideos.R
import com.sphere.shortvideos.baseui.GenericBindActivity
import com.sphere.shortvideos.databinding.ActivityMainBinding
import com.sphere.shortvideos.fragment.HomeFragment
import com.sphere.shortvideos.fragment.ProfileFragment
import com.sphere.shortvideos.fragment.VideoStreamFragment
import com.sphere.shortvideos.vm.MainViewModel

class MainActivity : GenericBindActivity<ActivityMainBinding>() {

    private val viewModel by viewModels<MainViewModel>()

    override val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun initUI() {
        setupBottomNav()
        viewModel.collectHistoryRoom()
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnApplyWindowInsetsListener(null)
        val fragments = listOf(HomeFragment(), VideoStreamFragment(), ProfileFragment())
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.offscreenPageLimit = fragments.size
        binding.viewPager.adapter = object : FragmentStateAdapter(supportFragmentManager, lifecycle) {
            override fun getItemCount(): Int = fragments.size
            override fun createFragment(position: Int): Fragment = fragments[position]
        }
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.bottomNav.menu[position].isChecked = true
            }
        })
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.tab_home -> binding.viewPager.setCurrentItem(0, false)
                R.id.tab_video -> binding.viewPager.setCurrentItem(1, false)
                R.id.tab_user -> binding.viewPager.setCurrentItem(2, false)
            }
            true
        }
        binding.viewPager.setCurrentItem(1, false)
    }

}