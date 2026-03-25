package com.sphere.shortvideos.dialogs.withdraw

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WithdrawalTaskItem(
    val text: String,
    val isCompleted: Boolean,
    val progressText: String = ""
) : Parcelable
