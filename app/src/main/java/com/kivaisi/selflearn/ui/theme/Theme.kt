package com.kivaisi.selflearn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PrimaryPurple = Color(0xFF5B3DF2)
val AccentYellow = Color(0xFFFFD400)
val AccentGreen = Color(0xFF2ECC71)
val AccentRed = Color(0xFFFF5B5B)
val BackgroundLight = Color(0xFFF7F5FF)
val CardSurface = Color(0xFFFFFFFF)

private val LightColors = lightColorScheme(
    primary = PrimaryPurple,
    secondary = AccentGreen,
    tertiary = AccentYellow,
    background = BackgroundLight,
    surface = CardSurface,
    error = AccentRed
)

@Composable
fun SelfLearnTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
