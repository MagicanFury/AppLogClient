package com.ztechno.applogclient.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Composable function to draw a custom circular progress UI.
 *
 * @param size The diameter of the circular progress UI.
 * @param strokeWidth The thickness of the progress stroke.
 * @param backgroundArcColor The color of the background arc that shows the full extent of the progress circle.
 */
@Composable
fun CustomerCircularProgressBar(
  size: Dp = 96.dp,
  strokeWidth: Dp = 12.dp,
  backgroundArcColor: Color = Color.LightGray
) {
  Canvas(modifier = Modifier.size(size)) {
    // Background Arc Implementation
    drawArc(
      color = backgroundArcColor,
      startAngle = 35f,
      sweepAngle = 360f,
      useCenter = false,
      size = Size(size.toPx(), size.toPx()),
      style = Stroke(width = strokeWidth.toPx())
    )
  }
}

@Preview
@Composable
fun CustomerCircularProgressBarPreview() {
  CustomerCircularProgressBar()
}