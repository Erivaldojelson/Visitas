package com.monst.transfiranow.ui

import androidx.activity.BackEventCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
internal fun PredictiveBackPreviewLayout(
    progress: Float,
    swipeEdge: Int,
    modifier: Modifier = Modifier,
    behind: @Composable () -> Unit,
    front: @Composable () -> Unit,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val direction = if (swipeEdge == BackEventCompat.EDGE_RIGHT) -1f else 1f
    val cornerRadius = 28.dp * clampedProgress

    BoxWithConstraints(modifier.fillMaxSize()) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }

        val frontTranslationX = direction * widthPx * clampedProgress
        val behindTranslationX = direction * -widthPx * 0.08f * (1f - clampedProgress)

        val frontScale = 1f - (0.04f * clampedProgress)
        val behindScale = 0.96f + (0.04f * clampedProgress)

        Box(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = behindTranslationX
                        scaleX = behindScale
                        scaleY = behindScale
                    }
            ) {
                behind()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = frontTranslationX
                        scaleX = frontScale
                        scaleY = frontScale
                        shape = RoundedCornerShape(cornerRadius)
                        clip = clampedProgress > 0f
                    }
            ) {
                front()
            }
        }
    }
}

