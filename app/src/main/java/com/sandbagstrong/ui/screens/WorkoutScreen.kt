package com.sandbagstrong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sandbagstrong.ui.WorkoutViewModel
import com.sandbagstrong.ui.components.ExerciseCard
import com.sandbagstrong.ui.components.RestTimerDialog
import com.sandbagstrong.ui.theme.AppColors

@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    onFinishEarly: () -> Unit = {},
    onBack: () -> Unit
) {
    val targets = viewModel.targets
    val type = viewModel.currentWorkoutType

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.bg)
            .padding(top = 8.dp)
    ) {
        // Top bar simple
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                color = AppColors.accent,
                fontSize = 24.sp,
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "WORKOUT ${type.name}",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
        }

        if (targets.isEmpty()) {
            Text("Loading program...", color = AppColors.muted, modifier = Modifier.padding(24.dp))
            return@Column
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(targets) { target ->
                val completed = viewModel.getCompletedCount(target.exercise.key)
                ExerciseCard(
                    exercise = target.exercise,
                    targetSets = target.sets,
                    targetReps = target.repsPerSet,
                    completedSets = completed,
                    restSeconds = target.restSeconds,
                    onCompleteSet = { viewModel.completeSet(target.exercise.key) },
                    isTimed = target.exercise.isTimed
                )
            }
        }

        // Finish button appears when all done
        if (viewModel.isWorkoutComplete()) {
            Spacer(Modifier.height(12.dp))
            com.sandbagstrong.ui.components.BigButton(
                text = "FINISH WORKOUT",
                onClick = { viewModel.showFeelRating = true },
                color = AppColors.completeGreen
            )
            Spacer(Modifier.height(20.dp))
        } else {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Complete all sets. The app will ask how it felt.",
                color = AppColors.muted,
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
            )
        }
    }

    // Rest timer overlay
    if (viewModel.showRestTimer) {
        RestTimerDialog(
            initialSeconds = viewModel.currentRestSeconds,
            onDismiss = { viewModel.dismissRestTimer() }
        )
    }

    // Feel rating sheet / overlay
    if (viewModel.showFeelRating) {
        FeelRatingScreen(
            onRate = { rating ->
                viewModel.finishWorkout(rating)
                onBack() // go home after save + auto advance
            }
        )
    }
}

