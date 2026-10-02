package com.budgetapp.app.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute {
    @Serializable
    data object EnvelopeList : AppRoute

    @Serializable
    data object EnvelopeManagement : AppRoute

    @Serializable
    data class EnvelopeDetail(val envelopeId: Long) : AppRoute

    @Serializable
    data object ReviewInbox : AppRoute

    @Serializable
    data object CsvImport : AppRoute

    @Serializable
    data object CategorizationRules : AppRoute

    @Serializable
    data object Analytics : AppRoute

    @Serializable
    data object ProfileMenu : AppRoute

    @Serializable
    data object Profile : AppRoute

    @Serializable
    data object About : AppRoute

    @Serializable
    data object Support : AppRoute

    @Serializable
    data object BackupRestore : AppRoute
}
