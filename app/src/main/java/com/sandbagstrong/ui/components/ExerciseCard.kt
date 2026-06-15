package com.sandbagstrong.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sandbagstrong.domain.Exercise
import com.sandbagstrong.ui.theme.AppColors

@Composable
fun ExerciseCard(
    exercise: Exercise,
    targetSets: Int,
    targetReps: Int,
    completedSets: Int,
    restSeconds: Int,
    onCompleteSet: () -> Unit,
    isTimed: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = if (targetSets > 0) completedSets.coerceAtMost(targetSets) else 0
    val isComplete = progress >= targetSets

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.surface)
            .padding(16.dp)
    ) {
        Text(
            text = exercise.name,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        if (exercise.notes.isNotBlank()) {
            Text(
                text = exercise.notes,
                color = AppColors.muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isTimed) exercise.reps else "$targetSets × $targetReps",
                color = AppColors.accent,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = if (isTimed) "DONE" else "$progress / $targetSets",
                color = if (isComplete) AppColors.completeGreen else AppColors.muted,
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        // Simple set indicators (circles)
        if (!isTimed) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                repeat(targetSets) { i ->
                    val filled = i < progress
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (filled) AppColors.completeGreen else Color(0xFF333333))
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        BigButton(
            text = if (isTimed) "COMPLETE" else "COMPLETE SET",
            onClick = onCompleteSet,
            enabled = !isComplete,
            color = if (isComplete) AppColors.completeGreen else AppColors.accent
        )

        if (!isTimed && restSeconds > 0) {
            Text(
                text = "Rest target: ${restSeconds}s",
                color = AppColors.muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp)
            )
        }
    }
}
