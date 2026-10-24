package com.sindantechnologies.gharhisab.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sindantechnologies.gharhisab.di.AppContainer
import com.sindantechnologies.gharhisab.ui.screens.budget.BudgetScreen
import com.sindantechnologies.gharhisab.ui.screens.budget.BudgetViewModel
import com.sindantechnologies.gharhisab.ui.screens.home.HomeScreen
import com.sindantechnologies.gharhisab.ui.screens.home.HomeViewModel
import com.sindantechnologies.gharhisab.ui.screens.onboarding.LanguageSelectionScreen
import com.sindantechnologies.gharhisab.ui.screens.onboarding.LanguageSelectionViewModel
import com.sindantechnologies.gharhisab.ui.screens.onboarding.StorageSelectionScreen
import com.sindantechnologies.gharhisab.ui.screens.onboarding.StorageSelectionViewModel
import com.sindantechnologies.gharhisab.ui.screens.onboarding.ThemeSelectionScreen
import com.sindantechnologies.gharhisab.ui.screens.onboarding.ThemeSelectionViewModel
import com.sindantechnologies.gharhisab.ui.screens.planned.PlannedExpensesScreen
import com.sindantechnologies.gharhisab.ui.screens.planned.PlannedExpensesViewModel
import com.sindantechnologies.gharhisab.ui.screens.settings.SettingsScreen
import com.sindantechnologies.gharhisab.ui.screens.splash.SplashScreen
import com.sindantechnologies.gharhisab.ui.screens.transaction.AddTransactionScreen
import com.sindantechnologies.gharhisab.ui.screens.transaction.AddTransactionViewModel
import com.sindantechnologies.gharhisab.ui.screens.transaction.EditTransactionScreen
import com.sindantechnologies.gharhisab.ui.screens.transaction.EditTransactionViewModel
import com.sindantechnologies.gharhisab.ui.screens.transaction.TransactionsScreen
import com.sindantechnologies.gharhisab.ui.screens.transaction.TransactionsViewModel

private const val NAV_ANIM_DURATION = 300

@Composable
fun GharHisabNavHost(
    appContainer: AppContainer,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(NAV_ANIM_DURATION),
            ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(NAV_ANIM_DURATION),
            ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(NAV_ANIM_DURATION),
            ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(NAV_ANIM_DURATION),
            ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION))
        },
    ) {
        composable(
            route = Screen.Splash.route,
            enterTransition = { fadeIn(animationSpec = tween(NAV_ANIM_DURATION)) },
            exitTransition = { fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) },
        ) {
            SplashScreen(
                userPreferencesRepository = appContainer.userPreferencesRepository,
                onNavigateToOnboarding = {
                    navController.navigate(Screen.LanguageSelection.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
            )
        }

        composable(route = Screen.LanguageSelection.route) {
            val viewModel: LanguageSelectionViewModel = viewModel(
                factory = LanguageSelectionViewModel.Factory(appContainer.userPreferencesRepository)
            )
            LanguageSelectionScreen(
                viewModel = viewModel,
                onContinue = { navController.navigate(Screen.ThemeSelection.route) },
            )
        }

        composable(route = Screen.ThemeSelection.route) {
            val viewModel: ThemeSelectionViewModel = viewModel(
                factory = ThemeSelectionViewModel.Factory(appContainer.userPreferencesRepository)
            )
            ThemeSelectionScreen(
                viewModel = viewModel,
                onContinue = { navController.navigate(Screen.StorageSelection.route) },
            )
        }

        composable(route = Screen.StorageSelection.route) {
            val viewModel: StorageSelectionViewModel = viewModel(
                factory = StorageSelectionViewModel.Factory(appContainer.userPreferencesRepository)
            )
            StorageSelectionScreen(
                viewModel = viewModel,
                onContinue = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.LanguageSelection.route) { inclusive = true }
                    }
                },
            )
        }

        composable(route = Screen.Home.route) {
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(
                    appContainer.transactionRepository,
                    appContainer.categoryRepository,
                    appContainer.budgetRepository,
                    appContainer.plannedExpenseRepository,
                    appContainer.userPreferencesRepository
                )
            )
            HomeScreen(
                viewModel = viewModel,
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToAddTransaction = { navController.navigate(Screen.AddTransaction.route) },
                onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                onNavigateToEditTransaction = { transactionId ->
                    navController.navigate(Screen.EditTransaction.createRoute(transactionId))
                },
                onNavigateToBudgets = { navController.navigate(Screen.Budgets.route) },
                onNavigateToPlannedExpenses = { navController.navigate(Screen.PlannedExpenses.route) }
            )
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(
                userPreferencesRepository = appContainer.userPreferencesRepository,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(route = Screen.Budgets.route) {
            val viewModel: BudgetViewModel = viewModel(
                factory = BudgetViewModel.Factory(
                    appContainer.budgetRepository,
                    appContainer.transactionRepository,
                    appContainer.categoryRepository
                )
            )
            BudgetScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.PlannedExpenses.route) {
            val viewModel: PlannedExpensesViewModel = viewModel(
                factory = PlannedExpensesViewModel.Factory(
                    appContainer.plannedExpenseRepository,
                    appContainer.transactionRepository,
                    appContainer.categoryRepository,
                    appContainer.userPreferencesRepository,
                    appContainer.context
                )
            )
            PlannedExpensesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.AddTransaction.route) {
            val viewModel: AddTransactionViewModel = viewModel(
                factory = AddTransactionViewModel.Factory(
                    appContainer.transactionRepository,
                    appContainer.categoryRepository
                )
            )
            AddTransactionScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.Transactions.route) {
            val viewModel: TransactionsViewModel = viewModel(
                factory = TransactionsViewModel.Factory(
                    appContainer.transactionRepository,
                    appContainer.categoryRepository
                )
            )
            TransactionsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditTransaction = { transactionId ->
                    navController.navigate(Screen.EditTransaction.createRoute(transactionId))
                }
            )
        }

        composable(
            route = Screen.EditTransaction.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
            val viewModel: EditTransactionViewModel = viewModel(
                factory = EditTransactionViewModel.Factory(
                    transactionId,
                    appContainer.transactionRepository,
                    appContainer.categoryRepository
                )
            )
            EditTransactionScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
