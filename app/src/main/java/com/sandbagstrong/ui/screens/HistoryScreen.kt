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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sandbagstrong.domain.FeelRating
import com.sandbagstrong.ui.WorkoutViewModel
import com.sandbagstrong.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: WorkoutViewModel,
    onBack: () -> Unit
) {
    val sessions by viewModel.historyFlow.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.bg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "← BACK",
                color = AppColors.accent,
                fontSize = 16.sp,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .clickable { onBack() }
            )
            Text(
                text = "HISTORY",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(16.dp))

        if (sessions.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No history yet.", color = AppColors.muted, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text("Complete your first workout to see it here.", color = AppColors.muted)
            }
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sessions) { s ->
                val date = SimpleDateFormat("EEE, MMM d  •  HH:mm", Locale.getDefault())
                    .format(Date(s.dateMillis))
                val feel = FeelRating.fromString(s.feelRating)
                val feelColor = when (feel) {
                    FeelRating.EASY -> AppColors.completeGreen
                    FeelRating.SOLID -> AppColors.accent
                    FeelRating.BRUTAL -> Color(0xFFFF6D00)
                    FeelRating.FAILED -> Color(0xFFE53935)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.surface)
                        .padding(16.dp)
                ) {
                    Row {
                        Text(
                            text = "WORKOUT ${s.workoutType}",
                            color = AppColors.accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = feel.display.uppercase(),
                            color = feelColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(date, color = AppColors.muted, fontSize = 13.sp)
                    if (s.durationSeconds > 0) {
                        val mins = s.durationSeconds / 60
                        val secs = s.durationSeconds % 60
                        Text("${mins}m ${secs}s", color = AppColors.muted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
