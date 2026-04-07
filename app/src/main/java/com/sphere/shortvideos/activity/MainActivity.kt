package com.sphere.shortvideos.activity

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.sphere.shortvideos.GlobalConstants
import com.sphere.shortvideos.R
import com.sphere.shortvideos.baseui.GenericBindActivity
import com.sphere.shortvideos.databinding.ActivityMainBinding
import com.sphere.shortvideos.dialogs.BackTipsDialogFragment
import com.sphere.shortvideos.dialogs.OpenNotificationDialogFragment
import com.sphere.shortvideos.fragment.HomeFragment
import com.sphere.shortvideos.fragment.ProfileFragment
import com.sphere.shortvideos.fragment.TaskFragment
import com.sphere.shortvideos.fragment.VideoStreamFragment
import com.sphere.shortvideos.fragment.WithdrawFragment
import com.sphere.shortvideos.helper.AppHelper
import com.sphere.shortvideos.helper.DialogFragmentDisplayHelper
import com.sphere.shortvideos.helper.HelperRewardShow
import com.sphere.shortvideos.helper.ad.AdUtils
import com.sphere.shortvideos.helper.mmkv.MMKVRepository
import com.sphere.shortvideos.helper.permission.PermissionHelper
import com.sphere.shortvideos.helper.permission.PostPermission
import com.sphere.shortvideos.logError
import com.sphere.shortvideos.notification.NotificationHelper
import com.sphere.shortvideos.view.SpineHelper
import com.sphere.shortvideos.vm.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : GenericBindActivity<ActivityMainBinding>() {
    private var moneyGe = -1.0
    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (PermissionHelper.areNotificationsEnabled(this)) {
            if (moneyGe > 0) {
                HelperRewardShow.addMoneyNotExChangeFlyAnim(moneyGe, 500)
            }
        }
    }
    private val h = SpineHelper()
    private val viewModel by viewModels<MainViewModel>()

    override val binding by lazy {
        topMar = 0
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun initUI() {
        setupBottomNav()
        selectForYouIfFromNotification(intent)
        consumeOpenWalletIntent(intent)
        viewModel.collectHistoryRoom()
        lifecycleScope.launch {
            delay(1000)
            NotificationHelper.showOrUpdateNotificationService(this@MainActivity)
        }
        if (MMKVRepository.isNewUser) {
            binding.ivFirstGuide.setOnClickListener { }
            binding.ivFirstGuide.visibility = View.VISIBLE
        }
        supportFragmentManager.registerFragmentLifecycleCallbacks(dialogLifecycleCallbacks, true)
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnApplyWindowInsetsListener(null)

        // 禁用自动 tint，手动控制颜色
        binding.bottomNav.itemIconTintList = null
        binding.bottomNav.itemTextColor = null
        val fragments =
            listOf(HomeFragment(), VideoStreamFragment(), WithdrawFragment(), TaskFragment(), ProfileFragment())
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
            setOther()
            when (item.itemId) {
                R.id.tab_home -> {
                    binding.bottomNav.menu.findItem(R.id.tab_home).icon =
                        ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_nav_home_selected)
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
        binding.bottomNav.selectedItemId = R.id.tab_video
        h.addViewWallet(binding.centerWallet, this)
        if (MMKVRepository.isNewUser.not()) {
            showNotificationOpen()
        }
        registerViewModel()
    }

    private fun setOther() {
        binding.bottomNav.menu.apply {
            findItem(R.id.tab_home).icon = R.drawable.ic_nav_home.fetchIcon()
            findItem(R.id.tab_user).icon = R.drawable.ic_nav_user.fetchIcon()
            findItem(R.id.tab_task).icon = R.drawable.ic_nav_task.fetchIcon()
            findItem(R.id.tab_video).icon = R.drawable.ic_nav_video.fetchIcon()
            h.addViewWallet(binding.centerWallet, this@MainActivity)
        }
    }

    private fun Int.fetchIcon(): Drawable? {
        return ContextCompat.getDrawable(this@MainActivity, this)
    }

    fun hideOrShowGuide(show: Boolean = true) {
        if (show) {
            viewModel.newUserProgress()
            binding.ivFirstGuide.setOnClickListener { }
            binding.ivFirstGuide.visibility = View.VISIBLE
        } else {
            showNotificationOpen()
            binding.ivFirstGuide.visibility = View.GONE
        }
    }

    fun showNotificationOpen(isHome: Boolean = true, delTime: Long = 3000) {
        lifecycleScope.launch {
            delay(Random.nextLong(2500, delTime))
            if (isFinishing || isDestroyed) return@launch
            if (DialogFragmentDisplayHelper.hasDialogFragmentShowing(this@MainActivity)) return@launch
            val isShow = if (isHome) PermissionHelper.isShowNotificationDialogHome(this@MainActivity)
            else PermissionHelper.isShowNotificationDialogAfterRv(this@MainActivity)
            if (isShow) {
                val notificationDialogFragment = OpenNotificationDialogFragment()
                notificationDialogFragment.onClaim = {
                    moneyGe = it
                    startForResult.launch(GoSettingAndCheckActivity.fetchIntent(activity, 0))
                }
                notificationDialogFragment.show(supportFragmentManager, "")
            }
        }
    }

    override fun onBackActioned() {
        if (MMKVRepository.isShowBackTips) {
            MMKVRepository.isShowBackTips = false
            BackTipsDialogFragment({ super.onBackActioned() }).show(supportFragmentManager, "onBack")
        } else {
            super.onBackActioned()
        }
    }

    private fun registerViewModel() {
        HelperRewardShow.registerConDialog(this)
        HelperRewardShow.registerGetMoney(this)
    }

    fun jumpWallet() {
        binding.bottomNav.selectedItemId = R.id.tab_wallet
    }

    /** 切换到短视频 Tab（内嵌 [PangleVideoContainerFragment] 列表） */
    fun jumpToVideoTab() {
        binding.bottomNav.selectedItemId = R.id.tab_video
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        selectForYouIfFromNotification(intent)
        consumeOpenWalletIntent(intent)
    }


    private fun selectForYouIfFromNotification(intent: Intent) {
        val notificationId = intent.getIntExtra(NotificationHelper.NOTIFICATION_ID_KEY, -1)
        if (notificationId != -1) {
            binding.bottomNav.selectedItemId = if (notificationId == NotificationHelper.NOTI_ID_FIXED) {
                R.id.tab_wallet
            } else {
                R.id.tab_video
            }
        }
    }

    /** 从播放页等通过 Intent 唤起并打开提现 Tab */
    private fun consumeOpenWalletIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(GlobalConstants.EXTRA_KEY_OPEN_WALLET, false) != true) return
        intent.removeExtra(GlobalConstants.EXTRA_KEY_OPEN_WALLET)
        binding.root.post { jumpWallet() }
    }

    override fun onResume() {
        super.onResume()
        AdUtils.run {
            unlockHolder.preloadIfCan()
            rewardHolder.preloadIfCan()
            launchHolder.preloadIfCan()
        }
        runCatching {
            viewModel.syncPauseDialogState(DialogFragmentDisplayHelper.countShowingPauseDialogFragments(
                supportFragmentManager))
        }
    }

    private val dialogLifecycleCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
            super.onFragmentCreated(fm, f, savedInstanceState)
            if (HelperRewardShow.isPauseFragment(f)) {
                binding.root.post {
                    viewModel.onPauseDialogShown()
                    logError("onFragmentCreated-->${viewModel.fragmentNum.value} --$f")
                }
            }
        }

        override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
            if (HelperRewardShow.isPauseFragment(f)) {
                binding.root.post {
                    viewModel.onPauseDialogDismissed()
                    logError("onFragmentViewDestroyed-->${viewModel.fragmentNum.value} --$f")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        supportFragmentManager.unregisterFragmentLifecycleCallbacks(dialogLifecycleCallbacks)
    }

}