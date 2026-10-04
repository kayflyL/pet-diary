package com.petdiary.parrot.feature.pets

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petdiary.parrot.data.PetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
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

    /** 裁剪结果保存到私有媒体目录，并写入该鸟档案的 avatarPath（列表/英雄卡/详情复用同图） */
    fun saveCropped(bitmap: android.graphics.Bitmap, onSaved: () -> Unit) {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                val dir = java.io.File(context.filesDir, "media").apply { mkdirs() }
                java.io.File(dir, "avatar_$petId.jpg").also { out ->
                    out.outputStream().use { stream ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)
                    }
                }
            }
            petRepository.setAvatar(petId, file.absolutePath)
            withContext(Dispatchers.Main) { onSaved() }
        }
    }
}

/** 裁剪头像页：选图（相册，进入即弹）→ 拖动定位/双指缩放 → ✓ 保存 1:1 方形裁切 */
@Composable
fun AvatarCropScreen(onDone: () -> Unit, vm: AvatarCropViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sourceUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var image by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val graphicsLayer = rememberGraphicsLayer()
    var saving by remember { mutableStateOf(false) }
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

    Column(Modifier.fillMaxSize().background(Color(0xFF0B0D0C))) {
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
                "裁剪头像",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
        }
        // 裁剪框（1:1）：只录制里面的图片内容，遮罩画在外面不进入裁剪结果
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clipToBounds(),
        ) {
            image?.let { bmp ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            graphicsLayer.record { this@drawWithContent.drawContent() }
                            drawContent()
                        },
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            },
                    )
                }
                // 遮罩：方框外压暗 + 白色边框
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen },
                ) {
                    drawRect(Color(0x99000000))
                    val side = min(size.width, size.height)
                    drawRect(
                        color = Color.Transparent,
                        topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f),
                        size = androidx.compose.ui.geometry.Size(side, side),
                        blendMode = androidx.compose.ui.graphics.BlendMode.Clear,
                    )
                    drawRect(
                        color = Color.White,
                        topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f),
                        size = androidx.compose.ui.geometry.Size(side, side),
                        style = Stroke(width = 5f),
                    )
                }
            } ?: Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text("选择一张照片开始裁剪", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        }
        // 保存：由方框屏幕坐标反算源图裁切区域
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
                val side = min(cw, ch)
                val cropped = android.graphics.Bitmap.createBitmap(bmp, x0, y0, side, side)
                vm.saveCropped(cropped) { onDone() }
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
            "拖动定位 · 双指缩放 · 方框内为保存范围",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 11.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        )
    }
}
