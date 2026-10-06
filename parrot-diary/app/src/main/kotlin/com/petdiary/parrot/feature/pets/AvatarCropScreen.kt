package com.petdiary.parrot.feature.pets

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petdiary.parrot.data.PetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class AvatarCropViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: android.content.Context,
    private val petRepository: PetRepository,
) : ViewModel() {
    val petId: String = checkNotNull(savedStateHandle["petId"])

    /** avatar=1:1 头像；hero=首页英雄卡宽幅取景图（所见即所得） */
    val mode: String = savedStateHandle["mode"] ?: "avatar"

    /** 裁剪结果保存到私有媒体目录；avatar 模式带焦点，hero 模式所见即所得存整图 */
    fun saveCropped(bitmap: android.graphics.Bitmap, focalX: Float?, focalY: Float?, onSaved: () -> Unit) {
        viewModelScope.launch {
            val name = if (mode == "hero") "hero_$petId.jpg" else "avatar_$petId.jpg"
            val file = withContext(Dispatchers.IO) {
                val dir = java.io.File(context.filesDir, "media").apply { mkdirs() }
                java.io.File(dir, name).also { out ->
                    out.outputStream().use { stream ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)
                    }
                }
            }
            if (mode == "hero") {
                petRepository.setHeroPath(petId, file.absolutePath)
            } else {
                petRepository.setAvatar(petId, file.absolutePath, focalX, focalY)
            }
            withContext(Dispatchers.Main) { onSaved() }
        }
    }
}

/**
 * 裁剪页（通用）：选图（相册，进入即弹）→ 拖动定位/双指缩放 → ✓ 保存。
 * avatar 模式裁 1:1 方形头像；hero 模式裁 1.9:1 宽幅，作为首页英雄卡背景（所见即所得）。
 */
@Composable
fun AvatarCropScreen(onDone: () -> Unit, vm: AvatarCropViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val isHero = vm.mode == "hero"
    val frameRatio = if (isHero) 1.9f else 1f
    var sourceUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var image by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var frameSize by remember { mutableStateOf(android.util.Size(0, 0)) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        sourceUri = uri
        image = null
        scale = 1f
        offset = Offset.Zero
    }
    // 进入即弹相册选择
    LaunchedEffect(Unit) { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    // 解码选中图（长边限 1600，避免大图卡顿）
    LaunchedEffect(sourceUri) {
        val u = sourceUri ?: return@LaunchedEffect
        image = withContext(Dispatchers.IO) {
            runCatching {
                val src = android.graphics.ImageDecoder.createSource(context.contentResolver, u)
                android.graphics.ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                    val maxSide = maxOf(info.size.width, info.size.height)
                    if (maxSide > 1600) {
                        decoder.setTargetSize(info.size.width * 1600 / maxSide, info.size.height * 1600 / maxSide)
                    }
                }
            }.getOrNull()
        }
        scale = 1f
        offset = Offset.Zero
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0D0C))
            .clickable(enabled = image == null) {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
    ) {
        // 顶栏
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "✕",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier
                    .clickable(onClick = onDone)
                    .padding(10.dp),
            )
            Text(
                if (isHero) "调整首页卡片取景" else "裁剪头像",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
        }
        // 裁剪框：所见即所得，框内即保存范围。
        // 图片按 Crop 实际尺寸铺出（比框大、居中），由框裁掉溢出——
        // 若用 fillMaxSize+Crop，框外像素会被提前裁掉，拖动时就露出黑底而不是照片其它部分。
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(frameRatio)
                .clipToBounds()
                .border(3.dp, Color.White)
                .onGloballyPositioned { frameSize = android.util.Size(it.size.width, it.size.height) }
                .pointerInput(image) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val bmp = image ?: return@detectTransformGestures
                        if (frameSize.width == 0 || frameSize.height == 0) return@detectTransformGestures
                        val k = maxOf(
                            frameSize.width.toFloat() / bmp.width,
                            frameSize.height.toFloat() / bmp.height,
                        )
                        val newScale = (scale * zoom).coerceIn(1f, 6f)
                        val maxX = (bmp.width * k * newScale - frameSize.width) / 2f
                        val maxY = (bmp.height * k * newScale - frameSize.height) / 2f
                        scale = newScale
                        offset = Offset(
                            (offset.x + pan.x).coerceIn(-maxX, maxX),
                            (offset.y + pan.y).coerceIn(-maxY, maxY),
                        )
                    }
                },
        ) {
            val density = LocalDensity.current
            image?.let { bmp ->
                if (frameSize.width > 0 && frameSize.height > 0) {
                    val k = maxOf(
                        frameSize.width.toFloat() / bmp.width,
                        frameSize.height.toFloat() / bmp.height,
                    )
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(with(density) { (bmp.width * k).toDp() }, with(density) { (bmp.height * k).toDp() })
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            },
                    )
                }
            } ?: Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "轻触此处选择一张照片",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                )
            }
        }
        // 保存：由框屏幕坐标反算源图裁切区域（框比例即裁剪比例）
        Button(
            onClick = {
                val bmp = image ?: return@Button
                val fw = frameSize.width.coerceAtLeast(1)
                val fh = frameSize.height.coerceAtLeast(1)
                val k = maxOf(fw.toFloat() / bmp.width, fh.toFloat() / bmp.height)
                fun toSrc(fx: Float, fy: Float): Pair<Int, Int> {
                    val dx = (fx - fw / 2f) / scale + fw / 2f - offset.x
                    val dy = (fy - fh / 2f) / scale + fh / 2f - offset.y
                    val sx = ((dx - (fw - bmp.width * k) / 2f) / k).roundToInt()
                    val sy = ((dy - (fh - bmp.height * k) / 2f) / k).roundToInt()
                    return sx to sy
                }
                val (sx0, sy0) = toSrc(0f, 0f)
                val (ex0, ey0) = toSrc(fw.toFloat(), fh.toFloat())
                val cw = (ex0 - sx0).coerceIn(1, bmp.width)
                val ch = (ey0 - sy0).coerceIn(1, bmp.height)
                val x0 = sx0.coerceIn(0, (bmp.width - cw).coerceAtLeast(0))
                val y0 = sy0.coerceIn(0, (bmp.height - ch).coerceAtLeast(0))
                val cropped = android.graphics.Bitmap.createBitmap(bmp, x0, y0, cw, ch)
                if (isHero) {
                    vm.saveCropped(cropped, null, null) { onDone() }
                } else {
                    val fx = ((x0 + cw / 2f) / bmp.width).coerceIn(0f, 1f)
                    val fy = ((y0 + ch / 2f) / bmp.height).coerceIn(0f, 1f)
                    vm.saveCropped(cropped, fx, fy) { onDone() }
                }
            },
            enabled = image != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F8F5B)),
        ) {
            Text("✓ 用这张", fontWeight = FontWeight.Bold)
        }
        Text(
            if (isHero) "拖动定位 · 双指缩放 · 框内就是首页卡片显示的画面"
            else "拖动定位 · 双指缩放 · 框内为头像保存范围",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 11.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        )
    }
}
