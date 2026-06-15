package com.sandbagstrong.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sandbagstrong.domain.FeelRating
import com.sandbagstrong.ui.components.BigButton
import com.sandbagstrong.ui.theme.AppColors

@Composable
fun FeelRatingScreen(
    onRate: (FeelRating) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.bg.copy(alpha = 0.98f))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "HOW DID THAT FEEL?",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Be honest. This drives progression.",
            color = AppColors.muted,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(32.dp))

        // Big buttons stacked
        listOf(
            FeelRating.EASY to "😌  EASY — Add volume or cut rest next time",
            FeelRating.SOLID to "💪  SOLID — Good. Repeat.",
            FeelRating.BRUTAL to "🔥  BRUTAL — Tough but you got it. Repeat.",
            FeelRating.FAILED to "😤  FAILED — Scale back 1 rep per set."
        ).forEach { (rating, label) ->
            BigButton(
                text = label,
                onClick = { onRate(rating) },
                color = when (rating) {
                    FeelRating.EASY -> AppColors.completeGreen
                    FeelRating.SOLID -> AppColors.accent
                    FeelRating.BRUTAL -> Color(0xFFFF6D00)
                    FeelRating.FAILED -> Color(0xFFC62828)
                }
            )
            Spacer(Modifier.height(14.dp))
        }
    }
}
