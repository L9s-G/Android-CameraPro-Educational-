package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 触摸对焦与测光指示器 (Focus Ring Indicator)
 *
 * 【教学点】：在用户点击屏幕取景器时，产生平滑的收缩聚焦动画，
 * 给予直观的 3A (AF/AE) 锁定反馈，契合专业相机的视觉语言。
 */
@Composable
fun FocusRing(
    point: Offset,
    modifier: Modifier = Modifier
) {
    val scaleAnim = remember { Animatable(1.5f) }
    val alphaAnim = remember { Animatable(1.0f) }

    LaunchedEffect(point) {
        scaleAnim.snapTo(1.6f)
        alphaAnim.snapTo(1.0f)
        scaleAnim.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
        )
    }

    val boxSize = 72.dp
    val boxSizePx = 72f * 2.5f // 参考像素换算

    Canvas(
        modifier = modifier
            .offset {
                IntOffset(
                    (point.x - boxSizePx / 2f).roundToInt(),
                    (point.y - boxSizePx / 2f).roundToInt()
                )
            }
            .size(boxSize)
    ) {
        val currentScale = scaleAnim.value
        val strokeWidth = 2.dp.toPx()
        val cornerLength = 14.dp.toPx() * currentScale
        val ringColor = Color(0xFFFFCA28).copy(alpha = alphaAnim.value) // 经典对焦金黄

        val w = size.width * currentScale
        val h = size.height * currentScale
        val left = (size.width - w) / 2f
        val top = (size.height - h) / 2f

        // 绘制对焦方框带圆角
        drawRoundRect(
            color = ringColor,
            topLeft = Offset(left, top),
            size = Size(w, h),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
            style = Stroke(width = strokeWidth)
        )

        // 绘制中心准心小十字
        val center = Offset(size.width / 2f, size.height / 2f)
        val crossHalf = 4.dp.toPx()
        drawLine(
            color = ringColor,
            start = Offset(center.x - crossHalf, center.y),
            end = Offset(center.x + crossHalf, center.y),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = ringColor,
            start = Offset(center.x, center.y - crossHalf),
            end = Offset(center.x, center.y + crossHalf),
            strokeWidth = strokeWidth
        )
    }
}
