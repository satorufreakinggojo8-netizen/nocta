package com.nocta.app.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nocta.app.data.remote.BackendConfig
import com.nocta.app.ui.components.NoctaBottomNavigation
import com.nocta.app.ui.guide.ShadeGuide
import com.nocta.app.ui.guide.ShadeGuideViewModel
import com.nocta.app.ui.guide.ShadeStage
import com.nocta.app.ui.screens.coach.CoachScreen
import com.nocta.app.ui.screens.details.PlanWhyScreen
import com.nocta.app.ui.screens.details.PrivacyScreen
import com.nocta.app.ui.screens.details.RecommendationDetailScreen
import com.nocta.app.ui.screens.details.ScoreHelpScreen
import com.nocta.app.ui.screens.details.SleepDebtScreen
import com.nocta.app.ui.screens.details.SleepDetailScreen
import com.nocta.app.ui.screens.home.HomeScreen
import com.nocta.app.ui.screens.insights.InsightsScreen
import com.nocta.app.ui.screens.journal.JournalScreen
import com.nocta.app.ui.screens.log.LogSleepScreen
import com.nocta.app.ui.screens.optimize.OptimizeScreen
import com.nocta.app.ui.screens.profile.ProfileScreen
import com.nocta.app.ui.screens.sleep.SleepScreen
import com.nocta.app.ui.theme.NoctaBackground

@Composable
fun NoctaNavGraph(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val baseRoute = ShadeGuide.baseRouteOf(currentRoute)

    val guideViewModel: ShadeGuideViewModel = hiltViewModel()
    val guideState by guideViewModel.uiState.collectAsState()

    // Advance the tip only when the screen really changes.
    LaunchedEffect(baseRoute) { guideViewModel.onRouteChanged(baseRoute) }

    val coachRoute = NoctaDestination.Coach.route

    Scaffold(
        containerColor = NoctaBackground,
        bottomBar = {
            if (currentRoute in bottomNavDestinations.map { it.route }) {
                NoctaBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigate = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(NoctaDestination.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            NavHost(
                navController = navController,
                startDestination = NoctaDestination.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(NoctaDestination.Home.route) {
                    HomeScreen(
                        onOpenSleepDetails = { sessionId ->
                            navController.navigate(
                                NoctaRoutes.SLEEP_DETAILS.replace("{sessionId}", sessionId)
                            )
                        },
                        onOpenWindDown = {
                            navController.navigate(NoctaDestination.Optimize.route) {
                                launchSingleTop = true
                            }
                        },
                        onLogSleep = { navController.navigate(NoctaRoutes.LOG_SLEEP) },
                        onOpenRecommendation = { navController.navigate(NoctaRoutes.RECOMMENDATION) }
                    )
                }
                composable(NoctaDestination.Sleep.route) {
                    SleepScreen(
                        onOpenSleepDetails = { sessionId ->
                            navController.navigate(
                                NoctaRoutes.SLEEP_DETAILS.replace("{sessionId}", sessionId)
                            )
                        },
                        onLogSleep = { navController.navigate(NoctaRoutes.LOG_SLEEP) },
                        onOpenJournal = { navController.navigate(NoctaRoutes.JOURNAL) },
                        onOpenScoreHelp = { navController.navigate(NoctaRoutes.SCORE_HELP) }
                    )
                }
                composable(NoctaRoutes.JOURNAL) {
                    JournalScreen(onBack = { navController.popBackStack() })
                }
                composable(NoctaDestination.Optimize.route) {
                    OptimizeScreen(
                        onOpenPlanWhy = { navController.navigate(NoctaRoutes.PLAN_WHY) }
                    )
                }
                composable(coachRoute) {
                    CoachScreen()
                }
                composable(NoctaDestination.Insights.route) {
                    InsightsScreen(
                        onOpenSleepDetails = { sessionId ->
                            navController.navigate(
                                NoctaRoutes.SLEEP_DETAILS.replace("{sessionId}", sessionId)
                            )
                        },
                        onOpenSleepDebt = { navController.navigate(NoctaRoutes.SLEEP_DEBT) },
                        onLogSleep = { navController.navigate(NoctaRoutes.LOG_SLEEP) }
                    )
                }
                composable(NoctaDestination.Profile.route) {
                    ProfileScreen(
                        onOpenPrivacy = { navController.navigate(NoctaRoutes.PRIVACY) }
                    )
                }
                composable(NoctaRoutes.LOG_SLEEP) {
                    LogSleepScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = {
                            navController.popBackStack()
                        }
                    )
                }
                // Legacy wind-down route: renders the Optimize routine directly so
                // any external caller lands on real content, not a placeholder.
                composable(NoctaRoutes.WIND_DOWN) {
                    OptimizeScreen(
                        onOpenPlanWhy = { navController.navigate(NoctaRoutes.PLAN_WHY) }
                    )
                }
                composable(
                    route = NoctaRoutes.SLEEP_DETAILS,
                    arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
                ) {
                    SleepDetailScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(NoctaRoutes.RECOMMENDATION) {
                    RecommendationDetailScreen(
                        onBack = { navController.popBackStack() },
                        onAskVela = { navController.navigate(coachRoute) }
                    )
                }
                composable(NoctaRoutes.SLEEP_DEBT) {
                    SleepDebtScreen(onBack = { navController.popBackStack() })
                }
                composable(NoctaRoutes.SCORE_HELP) {
                    ScoreHelpScreen(onBack = { navController.popBackStack() })
                }
                composable(NoctaRoutes.PLAN_WHY) {
                    PlanWhyScreen(onBack = { navController.popBackStack() })
                }
                composable(NoctaRoutes.PRIVACY) {
                    PrivacyScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    route = NoctaRoutes.COACH_ASK,
                    arguments = listOf(navArgument("prompt") { type = NavType.StringType })
                ) { entry ->
                    // The question is sent once by the screen itself; recomposition
                    // and rotation never resend it.
                    CoachScreen(
                        initialPrompt = entry.arguments?.getString("prompt"),
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // Shade's contextual stage: the character plus one short tip for the
            // screen the user is on. Hidden where it would fight the content
            // (the chat itself, the log form, and pure explanation pages).
            if (ShadeGuide.hasTips(baseRoute)) {
                ShadeStage(
                    baseRoute = baseRoute,
                    tip = guideState.tip,
                    guideEnabled = guideState.enabled,
                    detailExpanded = guideState.detailExpanded,
                    aiAvailable = BackendConfig.isConfigured,
                    onActivate = {
                        val tip = guideState.tip
                        if (tip != null) {
                            if (BackendConfig.isConfigured) {
                                navController.navigate(
                                    NoctaRoutes.COACH_ASK.replace(
                                        "{prompt}",
                                        Uri.encode(tip.coachPrompt)
                                    )
                                )
                            } else {
                                // No backend: show the app's own guidance instead of
                                // pretending an answer is on its way.
                                guideViewModel.toggleDetail()
                            }
                        }
                    },
                    onMute = guideViewModel::mute,
                    onOpenCoach = { navController.navigate(coachRoute) }
                )
            }
        }
    }
}
