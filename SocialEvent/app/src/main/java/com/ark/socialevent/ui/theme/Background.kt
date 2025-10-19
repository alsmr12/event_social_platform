package com.ark.socialevent.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.random.Random

@Composable
fun ThemedCircleBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                drawCircles(this)
            }
    ) {
        content()
    }
}

private fun drawCircles(drawScope: DrawScope) {
    val circleColor = LightPurpleA
    for (i in 1..10) {
        drawScope.drawCircle(
            color = circleColor,
            radius = Random.nextFloat() * 200 + 50,
            center = androidx.compose.ui.geometry.Offset(
                x = Random.nextFloat() * drawScope.size.width,
                y = Random.nextFloat() * drawScope.size.height
            ),
            alpha = Random.nextFloat() * 0.5f + 0.1f
        )
    }
}
