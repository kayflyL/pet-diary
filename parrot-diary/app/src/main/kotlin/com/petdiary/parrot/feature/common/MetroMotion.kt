package com.petdiary.parrot.feature.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import kotlinx.coroutines.delay

/**
 * Metro 动效三件套（Windows Phone 8.1 移植，2026-10-06 调研定案）：
 * fluidEnter 羽化列表 / tiltPress 按压倾斜 / flipOnValue 数字翻面。
 * 全部收敛：角度减半、时长 200-320ms，只动「动」不动布局。
 */

/** 羽化列表（WP 邮件 hub 的 Fluid List）：入场自下方 22dp 浮入，按序号每 40ms 错开 */
@Composable
fun Modifier.fluidEnter(index: Int): Modifier {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay((index.coerceAtMost(9)) * 40L)
        shown = true
    }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "fluid",
    )
    return graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 22.dp.toPx()
    }
}

/** 按压倾斜（WP Mango 的 Tilt）：朝触点方向微倾 2.5° 并缩到 0.98，100ms 按下 / 200ms 回弹 */
@Composable
fun Modifier.tiltPress(): Modifier {
    var pressed by remember { mutableStateOf(false) }
    var nx by remember { mutableFloatStateOf(0f) }
    var ny by remember { mutableFloatStateOf(0f) }
    val t by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(if (pressed) 100 else 200, easing = FastOutSlowInEasing),
        label = "tilt",
    )
    return this
        .graphicsLayer {
            rotationX = t * 2.5f * -ny
            rotationY = t * 2.5f * nx
            scaleX = 1f - 0.02f * t
            scaleY = 1f - 0.02f * t
            cameraDistance = 16f * density
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                nx = ((down.position.x / size.width) * 2f - 1f).coerceIn(-1f, 1f)
                ny = ((down.position.y / size.height) * 2f - 1f).coerceIn(-1f, 1f)
                pressed = true
                waitForUpOrCancellation()
                pressed = false
            }
        }
}

/**
 * 数字翻面（live tile 的微缩版）：值变化时绕 X 轴翻面交替新旧文本。
 * 用 Animatable 手动走两段：0→90 露棱，换字后 -90→0 转正。
 */
@Composable
fun FlipText(
    value: String,
    modifier: Modifier = Modifier,
    content: @Composable (String) -> Unit,
) {
    var shown by remember { mutableStateOf(value) }
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(value) {
        if (value == shown) return@LaunchedEffect
        rotation.animateTo(90f, tween(180, easing = FastOutSlowInEasing))
        shown = value
        rotation.snapTo(-90f)
        rotation.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
    }
    Box(modifier) {
        Box(
            Modifier.graphicsLayer {
                rotationX = rotation.value
                cameraDistance = 16f * density
                alpha = if (rotation.value >= 89f || rotation.value <= -89f) 0f else 1f
            },
        ) {
            content(shown)
        }
    }
}
