package com.budgetapp.app

import android.app.Application
import com.budgetapp.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class BudgetApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@BudgetApplication)
            modules(appModule)
        }
    }
}
