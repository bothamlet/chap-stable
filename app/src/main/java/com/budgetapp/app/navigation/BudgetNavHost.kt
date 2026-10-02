package com.budgetapp.app.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.budgetapp.app.ui.components.MainBottomNav
import com.budgetapp.app.ui.components.MainBottomNavDestination
import com.budgetapp.app.ui.screens.AboutScreen
import com.budgetapp.app.ui.screens.AddTransactionOverlay
import com.budgetapp.app.ui.screens.AnalyticsScreen
import com.budgetapp.app.ui.screens.BackupRestoreScreen
import com.budgetapp.app.ui.screens.CashTransactionEditorState
import com.budgetapp.app.ui.screens.CategorizationRulesScreen
import com.budgetapp.app.ui.screens.CsvImportScreen
import com.budgetapp.app.ui.screens.EnvelopeDetailScreen
import com.budgetapp.app.ui.screens.EnvelopeListScreen
import com.budgetapp.app.ui.screens.EnvelopeListViewModel
import com.budgetapp.app.ui.screens.EnvelopeManagementScreen
import com.budgetapp.app.ui.screens.ProfileMenuScreen
import com.budgetapp.app.ui.screens.ProfileScreen
import com.budgetapp.app.ui.screens.ReviewInboxScreen
import com.budgetapp.app.ui.screens.SupportScreen
import com.budgetapp.app.ui.theme.MockSurface
import org.koin.androidx.compose.koinViewModel

@Composable
fun BudgetNavHost() {
    val navController = rememberNavController()
    val dashboardViewModel: EnvelopeListViewModel = koinViewModel()
    val dashboardState by dashboardViewModel.state.collectAsState()
    var cashEditor by remember { mutableStateOf<CashTransactionEditorState?>(null) }
    var envelopeCreateRequest by remember { mutableIntStateOf(0) }
    var screenModalVisible by remember { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route.orEmpty()
    val activeDestination = when {
        route.contains("EnvelopeList") -> MainBottomNavDestination.Home
        route.contains("ProfileMenu") -> MainBottomNavDestination.Menu
        else -> MainBottomNavDestination.Other
    }
    LaunchedEffect(route) {
        screenModalVisible = false
    }

    fun openAddTransaction() {
        cashEditor = CashTransactionEditorState(
            envelopeId = dashboardState.envelopes.firstOrNull()?.id,
        )
    }

    fun runPrimaryAddAction() {
        if (route.contains("EnvelopeManagement")) {
            envelopeCreateRequest += 1
        } else {
            openAddTransaction()
        }
    }

    fun navigateHome() {
        navController.navigate(AppRoute.EnvelopeList) {
            popUpTo(AppRoute.EnvelopeList) {
                inclusive = false
            }
            launchSingleTop = true
        }
    }

    fun navigateBack() {
        if (!navController.popBackStack()) {
            navigateHome()
        }
    }

    fun navigateMenu() {
        if (!route.contains("ProfileMenu")) {
            navController.navigate(AppRoute.ProfileMenu) {
                launchSingleTop = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MockSurface)
            .clipToBounds(),
    ) {
        NavHost(
            navController = navController,
            startDestination = AppRoute.EnvelopeList,
            modifier = Modifier
                .fillMaxSize()
                .background(MockSurface)
                .clipToBounds(),
            enterTransition = {
                fadeIn(tween(durationMillis = 340, easing = EaseOut)) +
                    slideInVertically(tween(durationMillis = 340, easing = EaseOut)) { height ->
                        (height * 0.025f).toInt()
                    }
            },
            exitTransition = {
                fadeOut(tween(durationMillis = 220, easing = EaseIn))
            },
            popEnterTransition = {
                fadeIn(tween(durationMillis = 300, easing = EaseOut))
            },
            popExitTransition = {
                fadeOut(tween(durationMillis = 220, easing = EaseIn)) +
                    slideOutVertically(tween(durationMillis = 220, easing = EaseIn)) { height ->
                        (height * 0.02f).toInt()
                    }
            },
        ) {
            composable<AppRoute.EnvelopeList> {
                EnvelopeListScreen(
                    viewModel = dashboardViewModel,
                    onCreateEnvelope = { navController.navigate(AppRoute.EnvelopeManagement) },
                    onOpenEnvelope = { envelopeId -> navController.navigate(AppRoute.EnvelopeDetail(envelopeId)) },
                    onOpenInbox = { navController.navigate(AppRoute.ReviewInbox) },
                    onOpenMenu = { navController.navigate(AppRoute.ProfileMenu) },
                )
            }
            composable<AppRoute.EnvelopeManagement> {
                EnvelopeManagementScreen(
                    viewModel = koinViewModel(),
                    onBack = ::navigateBack,
                    createRequestKey = envelopeCreateRequest,
                    onModalVisibleChange = { screenModalVisible = it },
                )
            }
            composable<AppRoute.EnvelopeDetail> {
                val route = it.toRoute<AppRoute.EnvelopeDetail>()
                EnvelopeDetailScreen(
                    envelopeId = route.envelopeId,
                    viewModel = koinViewModel(),
                    onBack = ::navigateBack,
                    onModalVisibleChange = { screenModalVisible = it },
                )
            }
            composable<AppRoute.ReviewInbox> {
                ReviewInboxScreen(
                    viewModel = koinViewModel(),
                    onBack = ::navigateBack,
                )
            }
            composable<AppRoute.CsvImport> {
                CsvImportScreen(
                    viewModel = koinViewModel(),
                    onBack = ::navigateBack,
                    onBottomActionVisibleChange = { screenModalVisible = it },
                )
            }
            composable<AppRoute.CategorizationRules> {
                CategorizationRulesScreen(
                    viewModel = koinViewModel(),
                    onBack = ::navigateBack,
                    onModalVisibleChange = { screenModalVisible = it },
                )
            }
            composable<AppRoute.Analytics> {
                AnalyticsScreen(
                    viewModel = koinViewModel(),
                    onBack = ::navigateBack,
                    onImport = { navController.navigate(AppRoute.CsvImport) },
                )
            }
            composable<AppRoute.ProfileMenu> {
                ProfileMenuScreen(
                    viewModel = koinViewModel(),
                    onBack = ::navigateBack,
                    onProfile = { navController.navigate(AppRoute.Profile) },
                    onInbox = { navController.navigate(AppRoute.ReviewInbox) },
                    onEnvelopes = { navController.navigate(AppRoute.EnvelopeManagement) },
                    onImport = { navController.navigate(AppRoute.CsvImport) },
                    onRules = { navController.navigate(AppRoute.CategorizationRules) },
                    onAnalytics = { navController.navigate(AppRoute.Analytics) },
                    onBackup = { navController.navigate(AppRoute.BackupRestore) },
                    onAbout = { navController.navigate(AppRoute.About) },
                    onSupport = { navController.navigate(AppRoute.Support) },
                )
            }
            composable<AppRoute.Profile> {
                ProfileScreen(
                    viewModel = koinViewModel(),
                    onBack = ::navigateBack,
                )
            }
            composable<AppRoute.About> {
                AboutScreen(onBack = ::navigateBack)
            }
            composable<AppRoute.Support> {
                SupportScreen(onBack = ::navigateBack)
            }
            composable<AppRoute.BackupRestore> {
                BackupRestoreScreen(
                    viewModel = koinViewModel(),
                    onBack = ::navigateBack,
                    onModalVisibleChange = { screenModalVisible = it },
                )
            }
        }
        if (cashEditor == null && !screenModalVisible) {
            MainBottomNav(
                active = activeDestination,
                onHome = {
                    if (!route.contains("EnvelopeList")) {
                        navigateHome()
                    }
                },
                onAddTransaction = ::runPrimaryAddAction,
                onMenu = ::navigateMenu,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
        cashEditor?.let { editor ->
            AddTransactionOverlay(
                editor = editor,
                envelopes = dashboardState.envelopes,
                profileSettings = dashboardState.profileSettings,
                onEditorChange = { cashEditor = it },
                onDismiss = { cashEditor = null },
                onSave = { submittedEditor ->
                    val submitted = submittedEditor.copy(hasSubmitted = true)
                    cashEditor = submitted
                    dashboardViewModel.addCashTransaction(submitted) { cashEditor = null }
                },
            )
        }
    }
}
