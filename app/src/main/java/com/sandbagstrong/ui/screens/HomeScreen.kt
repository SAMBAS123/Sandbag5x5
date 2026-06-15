package com.sandbagstrong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sandbagstrong.data.db.WorkoutSessionEntity
import com.sandbagstrong.domain.FeelRating
import com.sandbagstrong.domain.WorkoutType
import com.sandbagstrong.ui.WorkoutViewModel
import com.sandbagstrong.ui.components.BigButton
import com.sandbagstrong.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: WorkoutViewModel,
    onStartWorkout: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val nextType = viewModel.currentWorkoutType
    val history by viewModel.historyFlow.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.bg)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        Text(
            text = "70 LB SANDBAG",
            color = AppColors.accent,
            fontSize = 14.sp,
            letterSpacing = 3.sp,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = "S A N D B A G   5 x 5",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(32.dp))

        // Today's / Next workout card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.surface)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "NEXT UP",
                    color = AppColors.muted,
                    fontSize = 13.sp,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "WORKOUT ${nextType.name}",
                    color = Color.White,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = if (nextType == WorkoutType.A) "Clean • Squat • Press • Row • Carry" else "Shoulder • Lunge • G2OS • Drag • Carry",
                    color = AppColors.muted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        BigButton(
            text = "START WORKOUT ${nextType.name}",
            onClick = onStartWorkout
        )

        Spacer(Modifier.height(32.dp))

        // Recent history (minimal)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECENT",
                color = AppColors.muted,
                fontSize = 13.sp,
                letterSpacing = 2.sp
            )
            Text(
                text = "SEE ALL",
                color = AppColors.accent,
                fontSize = 13.sp,
                modifier = Modifier.clickable { onOpenHistory() }
            )
        }

        Spacer(Modifier.height(8.dp))

        if (history.isEmpty()) {
            Text(
                text = "No sessions yet.\nTime to get to work.",
                color = AppColors.muted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(history.take(4)) { session ->
                    HistoryRow(session)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = "Fixed weight. Simple program. Show up.",
            color = AppColors.muted.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun HistoryRow(session: WorkoutSessionEntity) {
    val dateStr = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(session.dateMillis))
    val feel = FeelRating.fromString(session.feelRating)
    val emoji = when (feel) {
        FeelRating.EASY -> "😌"
        FeelRating.SOLID -> "💪"
        FeelRating.BRUTAL -> "🔥"
        FeelRating.FAILED -> "😤"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "W${session.workoutType}", color = AppColors.accent, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(12.dp))
        Text(text = dateStr, color = Color.White)
        Spacer(Modifier.weight(1f))
        Text(text = emoji, fontSize = 18.sp)
    }
}
