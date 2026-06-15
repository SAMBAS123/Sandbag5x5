package com.sandbagstrong.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sandbagstrong.ui.theme.AppColors
import kotlinx.coroutines.delay

@Composable
fun RestTimerDialog(
    initialSeconds: Int,
    onDismiss: () -> Unit,
    onSkip: () -> Unit = onDismiss
) {
    var secondsLeft by remember(initialSeconds) { mutableIntStateOf(initialSeconds) }

    LaunchedEffect(initialSeconds) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
        // Auto dismiss when timer hits zero
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.bg.copy(alpha = 0.98f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "REST",
                    color = AppColors.muted,
                    fontSize = 18.sp,
                    letterSpacing = 4.sp
                )
                Spacer(Modifier.height(12.dp))

                Text(
                    text = String.format("%02d:%02d", secondsLeft / 60, secondsLeft % 60),
                    color = Color.White,
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = onSkip,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.surface,
                        contentColor = Color.White
                    )
                ) {
                    Text("SKIP REST", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Breathe. Reset. Next set.",
                    color = AppColors.muted,
                    fontSize = 14.sp
                )
            }
        }
    }
}
