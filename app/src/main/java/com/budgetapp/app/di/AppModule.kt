package com.budgetapp.app.di

import android.content.Context
import androidx.room.Room
import com.budgetapp.app.data.BudgetDatabase
import com.budgetapp.app.data.backup.EncryptedBackupCodec
import com.budgetapp.app.data.csv.CsvParser
import com.budgetapp.app.data.repository.BudgetRepository
import com.budgetapp.app.domain.CategorizationEngine
import com.budgetapp.app.security.LocalSecretStore
import com.budgetapp.app.security.SqlCipherDatabaseSupport
import com.budgetapp.app.ui.screens.AnalyticsViewModel
import com.budgetapp.app.ui.screens.BackupRestoreViewModel
import com.budgetapp.app.ui.screens.CategorizationRulesViewModel
import com.budgetapp.app.ui.screens.EnvelopeDetailViewModel
import com.budgetapp.app.ui.screens.EnvelopeListViewModel
import com.budgetapp.app.ui.screens.EnvelopeManagementViewModel
import com.budgetapp.app.ui.screens.CsvImportViewModel
import com.budgetapp.app.ui.screens.ProfileViewModel
import com.budgetapp.app.ui.screens.ReviewInboxViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        LocalSecretStore(
            androidContext().getSharedPreferences(
                "local_secrets",
                Context.MODE_PRIVATE,
            ),
        )
    }
    single {
        val context = androidContext()
        val passphrase = get<LocalSecretStore>().getOrCreateDatabasePassphrase()
        Room.databaseBuilder(
            context,
            BudgetDatabase::class.java,
            "budget.db",
        )
            .openHelperFactory(
                SqlCipherDatabaseSupport.createFactory(
                    context = context,
                    databaseName = "budget.db",
                    passphrase = passphrase,
                ),
            )
            .addMigrations(
                BudgetDatabase.Migration2To3,
                BudgetDatabase.Migration3To4,
                BudgetDatabase.Migration4To5,
            )
            .build()
    }

    single { CsvParser() }
    single { CategorizationEngine() }
    single { EncryptedBackupCodec() }
    single {
        androidContext().getSharedPreferences(
            "profile_settings",
            Context.MODE_PRIVATE,
        )
    }
    single { BudgetRepository(get(), get(), get(), get(), get()) }

    viewModel { EnvelopeListViewModel(get()) }
    viewModel { EnvelopeManagementViewModel(get()) }
    viewModel { EnvelopeDetailViewModel(get()) }
    viewModel { ReviewInboxViewModel(get()) }
    viewModel { CsvImportViewModel(get(), get()) }
    viewModel { CategorizationRulesViewModel(get()) }
    viewModel { AnalyticsViewModel(get()) }
    viewModel { ProfileViewModel(get()) }
    viewModel { BackupRestoreViewModel(get()) }
}
