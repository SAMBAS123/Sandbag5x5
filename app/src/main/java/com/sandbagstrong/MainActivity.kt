package com.sandbagstrong

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sandbagstrong.ui.MicroRoundViewModel
import com.sandbagstrong.ui.WorkoutViewModel
import com.sandbagstrong.ui.screens.HistoryScreen
import com.sandbagstrong.ui.screens.HomeScreen
import com.sandbagstrong.ui.screens.MicroRoundScreen
import com.sandbagstrong.ui.screens.WorkoutScreen
import com.sandbagstrong.ui.theme.SandbagTheme

class MainActivity : ComponentActivity() {

    private val app get() = application as SandbagApplication

    // Custom factory so we can inject our simple repository from the app class
    private val workoutViewModel: WorkoutViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WorkoutViewModel(app.repository) as T
            }
        }
    }

    private val microRoundViewModel: MicroRoundViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MicroRoundViewModel(app.repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SandbagTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SandbagNavHost(viewModel = workoutViewModel, microRoundViewModel = microRoundViewModel)
                }
            }
        }
    }
}

@Composable
fun SandbagNavHost(
    viewModel: WorkoutViewModel,
    microRoundViewModel: MicroRoundViewModel,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onStartWorkout = { navController.navigate("workout") },
                onOpenHistory = { navController.navigate("history") },
                onStartMicroRound = { navController.navigate("micro_round") },
                onNeedCorner = { navController.navigate("micro_round") }
            )
        }
        composable("workout") {
            WorkoutScreen(
                viewModel = viewModel,
                onBack = {
                    // If user backs out mid-workout we just pop; state stays in vm until finish
                    navController.popBackStack()
                }
            )
        }
        composable("micro_round") {
            MicroRoundScreen(
                viewModel = microRoundViewModel,
                onBack = {
                    microRoundViewModel.resetLocalState()
                    navController.popBackStack()
                },
                onFinished = {
                    viewModel.refreshWorkout()
                    navController.navigate("history") {
                        popUpTo("home")
                    }
                }
            )
        }
        composable("history") {
            HistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
