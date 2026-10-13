package com.kivaisi.selflearn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kivaisi.selflearn.ui.HomeScreen
import com.kivaisi.selflearn.ui.QuizScreen
import com.kivaisi.selflearn.ui.ReadingScreen
import com.kivaisi.selflearn.ui.ResultsScreen
import com.kivaisi.selflearn.ui.TimerSetupScreen
import com.kivaisi.selflearn.ui.theme.SelfLearnTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SelfLearnTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(onChapterClick = { chapterId ->
                                navController.navigate("timer_setup/$chapterId")
                            })
                        }
                        composable(
                            route = "timer_setup/{chapterId}",
                            arguments = listOf(navArgument("chapterId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: return@composable
                            TimerSetupScreen(
                                chapterId = chapterId,
                                onStart = { seconds ->
                                    navController.navigate("reading/$chapterId/$seconds")
                                }
                            )
                        }
                        composable(
                            route = "reading/{chapterId}/{timerSeconds}",
                            arguments = listOf(
                                navArgument("chapterId") { type = NavType.StringType },
                                navArgument("timerSeconds") { type = NavType.IntType }
                            )
                        ) { backStackEntry ->
                            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: return@composable
                            val timerSeconds = backStackEntry.arguments?.getInt("timerSeconds") ?: 300
                            ReadingScreen(
                                chapterId = chapterId,
                                timerSeconds = timerSeconds,
                                onStartQuiz = {
                                    navController.navigate("quiz/$chapterId")
                                }
                            )
                        }
                        composable(
                            route = "quiz/{chapterId}",
                            arguments = listOf(navArgument("chapterId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: return@composable
                            QuizScreen(
                                chapterId = chapterId,
                                onFinished = {
                                    navController.navigate("results/$chapterId") {
                                        popUpTo("home")
                                    }
                                },
                                onRetry = {
                                    navController.navigate("quiz/$chapterId") {
                                        popUpTo("timer_setup/$chapterId")
                                    }
                                }
                            )
                        }
                        composable(
                            route = "results/{chapterId}",
                            arguments = listOf(navArgument("chapterId") { type = NavType.StringType })
                        ) {
                            ResultsScreen(
                                onBackHome = {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },
                                onRetryChapter = { chapterId ->
                                    navController.navigate("quiz/$chapterId")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
