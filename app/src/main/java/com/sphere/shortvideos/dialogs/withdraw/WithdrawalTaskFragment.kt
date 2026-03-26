package com.sphere.shortvideos.dialogs.withdraw

import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.sphere.shortvideos.R
import com.sphere.shortvideos.databinding.DialogWithdrawalTaskBinding
import com.sphere.shortvideos.databinding.ItemWithdrawalTaskBinding
import com.sphere.shortvideos.helper.localEvent

/**
 * 提现任务弹窗。
 *
 * 注意：必须保留无参构造函数，避免 Fragment 重建时反射失败。
 */
class WithdrawalTaskFragment : DialogFragment() {
    var onConfirm: (() -> Unit)? = null
    var onClose: (() -> Unit)? = null

    private var binding: DialogWithdrawalTaskBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.TaskInfoDialogTheme)
        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DialogWithdrawalTaskBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentBinding = binding ?: return

        val args = arguments
        val type = args?.getString(ARG_TYPE).orEmpty()
        if (type.isNotBlank()) {
            localEvent("withdrawal_task_s", hashMapOf("type" to type))
        }
        currentBinding.tvTitle.text = args?.getString(ARG_TITLE).orEmpty()
        currentBinding.tvDesc.text = args?.getString(ARG_DESC).orEmpty()

        val taskList = args?.readTaskItems().orEmpty()
        bindTaskItem(currentBinding.taskItem1, taskList.getOrNull(0))
        bindTaskItem(currentBinding.taskItem2, taskList.getOrNull(1))

        currentBinding.btnConfirm.setOnClickListener {
            onConfirm?.invoke()
            dismissAllowingStateLoss()
        }
        currentBinding.ivClose.setOnClickListener {
            onClose?.invoke()
            dismissAllowingStateLoss()
        }
    }

    private fun bindTaskItem(itemBinding: ItemWithdrawalTaskBinding, item: WithdrawalTaskItem?) {
        if (item == null) {
            itemBinding.root.isVisible = false
            return
        }
        itemBinding.root.isVisible = true
        itemBinding.tvTask.text = item.text
        itemBinding.tvProgress.text = item.progressText
        itemBinding.tvProgress.isVisible = !item.isCompleted
        itemBinding.ivDone.isVisible = item.isCompleted
        itemBinding.tvTask.setTextColor(
            requireContext().getColor(
                if (item.isCompleted) R.color.color_primary else android.R.color.white
            )
        )
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.attributes = window.attributes.apply { gravity = Gravity.CENTER }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        private const val ARG_TITLE = "arg_title"
        private const val ARG_DESC = "arg_desc"
        private const val ARG_TASK_LIST = "arg_task_list"
        private const val ARG_TYPE = "arg_type"

        fun newInstance(title: String, desc: String, tasks: List<WithdrawalTaskItem>, type: String): WithdrawalTaskFragment {
            return WithdrawalTaskFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_DESC, desc)
                    putParcelableArrayList(ARG_TASK_LIST, ArrayList(tasks.take(2)))
                    putString(ARG_TYPE, type)
                }
            }
        }

        @Suppress("DEPRECATION")
        private fun Bundle.readTaskItems(): ArrayList<WithdrawalTaskItem> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getParcelableArrayList(ARG_TASK_LIST, WithdrawalTaskItem::class.java) ?: arrayListOf()
            } else {
                getParcelableArrayList(ARG_TASK_LIST) ?: arrayListOf()
            }
        }
    }
}
