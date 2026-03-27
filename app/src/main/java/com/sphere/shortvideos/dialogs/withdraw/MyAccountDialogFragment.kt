package com.sphere.shortvideos.dialogs.withdraw

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.graphics.toColorInt
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sphere.shortvideos.R
import com.sphere.shortvideos.databinding.DialogMyAccountBinding
import com.sphere.shortvideos.helper.MoneyCacheHelper
import com.sphere.shortvideos.helper.WithdrawAmountHelper
import com.sphere.shortvideos.helper.localEvent
import com.sphere.shortvideos.helper.withdraw.db.WithdrawalRecordEntity
import com.sphere.shortvideos.helper.withdraw.db.WithdrawalRecordStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyAccountDialogFragment : DialogFragment() {
    private var binding: DialogMyAccountBinding? = null
    private val adapter = MyAccountRecordAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.TransparentMaterialDialog)
        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogMyAccountBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        localEvent("withdrawal_information")
        val b = binding ?: return
        b.ivClose.setOnClickListener { dismissAllowingStateLoss() }
        b.rv.layoutManager = LinearLayoutManager(requireContext())
        b.rv.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val rows = WithdrawalRecordStore.getAllRecordsForHistory().map { it.toRow() }
            withContext(Dispatchers.Main) {
                adapter.submitList(rows)
            }
        }
    }

    private fun WithdrawalRecordEntity.toRow(): MyAccountRecordRow {
        val fmt = SimpleDateFormat("yyyy.M.d", Locale.getDefault())
        val dateText = fmt.format(Date(createdAt))
        val amountText = WithdrawAmountHelper.moneyFormatAddUnitWithNoSpace(
            MoneyCacheHelper.usdToShowMoneyD(withdrawalAmount),
        )
        val done = progress >= 1.0 - 1e-9
        return MyAccountRecordRow(
            dateText = dateText,
            amountText = amountText,
            statusText = if (done) "Done" else "Processing",
            statusColor = if (done) "#37C049".toColorInt() else "#20B1F5".toColorInt(),
        )
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.attributes = window.attributes.apply { gravity = Gravity.BOTTOM }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}

