package com.sphere.shortvideos.dialogs

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.sphere.shortvideos.R
import com.sphere.shortvideos.databinding.DialogBackTipsBinding
import com.sphere.shortvideos.helper.HelperRewardShow
import com.sphere.shortvideos.helper.MoneyCacheHelper
import com.sphere.shortvideos.helper.RemoteConfHelper
import com.sphere.shortvideos.helper.localEvent
import com.sphere.shortvideos.helper.reward.RewardHelper
import kotlinx.coroutines.launch

/**
 * Date：2026/1/23
 * Describe: Back tips bottom dialog
 */
class BackTipsDialogFragment(val onExit: () -> Unit) : DialogFragment() {

    private var _binding: DialogBackTipsBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.TransparentMaterialDialog)
        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogBackTipsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        localEvent("exit_pop")
        val info = RewardHelper.getConfigByLanguage().getExitReward(MoneyCacheHelper.fetchCurMoney())
        val textDex = getString(R.string.back_tips_desc, info.second)
        val text2 = getString(R.string.back_tips_stay, info.second)
        binding.tvDesc.text = textDex
        binding.btnStay.text = text2
        binding.btnExit.setOnClickListener {
            onExit.invoke()
            dismissAllowingStateLoss()
        }
        binding.btnStay.setOnClickListener {
            localEvent("exit_pop_c")
            dismissAllowingStateLoss()
            HelperRewardShow.addMoneyNotExChangeFlyAnim(info.first,600)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.attributes = window.attributes.apply { gravity = Gravity.BOTTOM }
        }
        dialog?.setCanceledOnTouchOutside(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
