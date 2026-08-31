package com.kuronoa.expensetracker.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.kuronoa.expensetracker.KuronoaApp

/** Factory generik (tanpa Hilt) yang membuat ViewModel dari dependensi di [KuronoaApp]. */
class AppViewModelFactory(
    private val app: KuronoaApp,
    private val create: (KuronoaApp) -> ViewModel
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return create(app) as T
    }
}
