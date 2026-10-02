package com.petdiary.parrot.feature.memories

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/** 地点大字列表的轮换色（设计文档 §7：绿/蓝/琥珀交替） */
private val placeColors = listOf(Color(0xFF2F8F5B), Color(0xFF4A7FB5), Color(0xFFB07A16))

/** 回忆 · 纸面（APP设计文档.md §7，StoryTeller 结构）· 数据来自 MemoriesViewModel */
@Composable
fun MemoriesScreen(vm: MemoriesViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    var seg by remember { mutableIntStateOf(0) }

    PaperTheme {
        Scaffold { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(PaperBg)
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
            ) {
                Text(
                    "MEMORIES",
                    fontSize = 10.5.sp,
                    letterSpacing = 2.5.sp,
                    color = GreenInk,
                    fontWeight = FontWeight.Bold,
                )
                Text("回忆", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 4.dp))
                Text(
                    "${ui.stories.size} 个故事 · ${ui.photos.size} 张照片 · ${ui.geotagged.size} 个足迹",
                    fontSize = 12.sp,
                    color = SubInk,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(bottom = 14.dp)) {
                    listOf("故事", "照片", "行迹").forEachIndexed { i, label ->
                        FilterChip(selected = seg == i, onClick = { seg = i }, label = { Text(label) })
                    }
                }

                when (seg) {
                    0 -> StoryList(ui.stories)
                    1 -> PhotoWall(ui.photos)
                    else -> TrailMap(ui.geotagged)
                }
            }
        }
    }
}

/** 故事卡：照片全出血 + 标题压图（StoryTeller "BEACH — 18 IMAGES" 式） */
@Composable
private fun StoryCard(story: StoryUi) {
    Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Text(
            "${story.date.monthValue} 月 ${story.date.dayOfMonth} 日 · " + story.petNames.joinToString("、"),
            style = MaterialTheme.typography.labelMedium,
            color = SubInk,
            modifier = Modifier.padding(bottom = 7.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.45f)
                .clip(RoundedCornerShape(18.dp))
                .background(GreenInkSoft),
        ) {
            if (story.firstPhotoPath != null) {
                AsyncImage(
                    model = java.io.File(story.firstPhotoPath),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFBFE3CE), Color(0xFF5FA97E))),
                        ),
                )
            }
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xAA10291A))))
                    .padding(12.dp),
            ) {
                Text(
                    when {
                        story.texts.isNotEmpty() -> story.texts.first().take(16)
                        else -> story.date.let { "${it.monthValue} 月 ${it.dayOfMonth} 日" }
                    },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
                Text(
                    "${story.entryCount} 条日记 · ${story.photoCount} 张照片",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
        // 卡下摘要只放第二条日记（第一条已压在图上，避免同句重复）
        story.texts.getOrNull(1)?.let { t ->
            Text(
                t,
                fontSize = 13.sp,
                color = SubInk,
                modifier = Modifier.padding(top = 8.dp),
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun StoryList(stories: List<StoryUi>) {
    if (stories.isEmpty()) {
        Text(
            "还没有故事。在「今天」写一条日记，同一天的照片和日记会自动打包成一个故事。",
            style = MaterialTheme.typography.bodyMedium,
            color = SubInk,
        )
        return
    }
    stories.forEach { story ->
        StoryCard(story)
    }
}

/** 照片墙：按月分组的方片网格 */
@Composable
private fun PhotoWall(photos: List<PhotoUi>) {
    if (photos.isEmpty()) {
        Text("随手拍的照片会按月聚在这里", style = MaterialTheme.typography.bodyMedium, color = SubInk)
        return
    }
    val byMonth = photos.groupBy { it.date.withDayOfMonth(1) }.toSortedMap(compareByDescending { it.year * 100 + it.monthValue })
    byMonth.forEach { (month, list) ->
        Text(
            "${month.year} 年 ${month.monthValue} 月 · ${list.size} 张",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
        )
        val rows = list.chunked(3)
        rows.forEach { rowPhotos ->
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowPhotos.forEach { p ->
                    AsyncImage(
                        model = java.io.File(p.path),
                        contentDescription = p.petName,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GreenInkSoft),
                        contentScale = ContentScale.Crop,
                    )
                }
                repeat(3 - rowPhotos.size) {
                    Spacer(Modifier.weight(1f).aspectRatio(1f))
                }
            }
        }
    }
}

/**
 * 高德瓦片源（国内直连、中文标注、无需 Key）。
 * 注意：osmdroid 的 XYTileSource 不会替换 {x}/{y}/{z} 占位符，而是把 "/z/x/y.png"
 * 直接追加到 URL 末尾——带 query 的模板写法会拼出 404。必须继承
 * OnlineTileSourceBase 自己把 z/x/y 拼进 query（评审 2026-10-03 实锤的"假地图"根因）。
 */
private fun amapTileSource() = object : OnlineTileSourceBase(
    "AMap",
    0,
    19,
    256,
    ".png",
    arrayOf(
        "https://webrd01.is.autonavi.com/appmaptile",
        "https://webrd02.is.autonavi.com/appmaptile",
        "https://webrd03.is.autonavi.com/appmaptile",
        "https://webrd04.is.autonavi.com/appmaptile",
    ),
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val z = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return baseUrl + "?lang=zh_cn&size=1&scale=1&style=7&x=$x&y=$y&z=$z"
    }
}

/** 行迹地图上的足迹点 */
private data class GeoPhoto(val lat: Double, val lng: Double, val photo: PhotoUi)

/** 按地点聚合的行迹列表行（设计文档 §7：大字地点列表，无卡片框） */
private data class PlaceGroup(val name: String, val cover: String, val photos: Int, val days: Int)

private fun groupPlaces(points: List<GeoPhoto>): List<PlaceGroup> =
    points
        .groupBy { it.photo.placeName ?: "%.3f,%.3f".format(it.lat, it.lng) }
        .map { (_, list) ->
            PlaceGroup(
                name = list.first().photo.placeName ?: "%.4f, %.4f".format(list.first().lat, list.first().lng),
                cover = list.first().photo.path,
                photos = list.size,
                days = list.map { it.photo.date }.distinct().size,
            )
        }
        .sortedByDescending { it.photos }

/** 照片裁圆 + 白描边 → 圆形足迹钉图标（替代 osmdroid 默认大头钉） */
private fun circlePhotoDrawable(ctx: Context, path: String, sizeDp: Float): Drawable? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val density = ctx.resources.displayMetrics.density
    val sizePx = (sizeDp * density).toInt().coerceIn(32, 160)
    val sample = maxOf(1, minOf(bounds.outWidth, bounds.outHeight) / (sizePx * 2))
    val bmp = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    val side = minOf(bmp.width, bmp.height)
    val sq = Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
    val ring = 2.5f * density
    val radius = sizePx / 2f - ring
    val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = BitmapShader(sq, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(Matrix().apply { setScale(radius * 2 / side, radius * 2 / side); postTranslate(ring, ring) })
        }
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, radius, fill)
    canvas.drawCircle(
        sizePx / 2f,
        sizePx / 2f,
        radius,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = ring
            color = android.graphics.Color.WHITE
        },
    )
    return BitmapDrawable(ctx.resources, out)
}

/** 行迹地图：高德真地图 + 白描边轨迹线 + 照片圆形足迹钉 + 地点大字列表 */
@Composable
private fun TrailMap(geotagged: List<PhotoUi>) {
    val points = geotagged.mapNotNull { p ->
        val lat = p.lat ?: return@mapNotNull null
        val lng = p.lng ?: return@mapNotNull null
        GeoPhoto(lat, lng, p)
    }
    if (points.isEmpty()) {
        Text(
            "拍照会自动记录拍摄地点（EXIF）——有了第一张带位置的照片，这里就会亮起足迹地图",
            style = MaterialTheme.typography.bodyMedium,
            color = SubInk,
            modifier = Modifier.padding(vertical = 14.dp),
        )
        return
    }
    val ctx = LocalContext.current
    val mapRadiusPx = 18f * ctx.resources.displayMetrics.density
    val mapView = remember {
        org.osmdroid.config.Configuration.getInstance().apply {
            userAgentValue = ctx.packageName
            osmdroidBasePath = java.io.File(ctx.filesDir, "osmdroid")
            osmdroidTileCache = java.io.File(ctx.cacheDir, "osmdroid")
        }
        MapView(ctx).apply {
            setTileSource(amapTileSource())
            setMultiTouchControls(true)
            // 互操作层不响应 Compose 的 clip；圆角与边界裁剪只能在 View 自身做
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, mapRadiusPx)
                }
            }
        }
    }
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        factory = { c ->
            mapView.apply {
                controller.setZoom(13.0)
                val centerLat = points.map { it.lat }.average()
                val centerLng = points.map { it.lng }.average()
                controller.setCenter(GeoPoint(centerLat, centerLng))
            }
        },
        update = { map ->
            map.overlays.clear()
            // 轨迹线：白色描边打底 + 草木绿主线（圆头圆拐角）
            listOf(13f to android.graphics.Color.WHITE, 8f to GreenInk.toArgb()).forEach { (width, color) ->
                map.overlays.add(
                    Polyline(map).apply {
                        setPoints(points.map { GeoPoint(it.lat, it.lng) })
                        outlinePaint.strokeWidth = width
                        outlinePaint.color = color
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                        outlinePaint.strokeJoin = Paint.Join.ROUND
                    },
                )
            }
            points.forEach { g ->
                map.overlays.add(
                    Marker(map).apply {
                        position = GeoPoint(g.lat, g.lng)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        circlePhotoDrawable(ctx, g.photo.path, 40f)?.let { icon = it }
                        title = g.photo.placeName ?: "拍摄地点"
                        subDescription = SimpleDateFormat("M 月 d 日", Locale.CHINESE).format(Date(g.photo.date.toEpochDay() * 86400000L))
                    },
                )
            }
            map.invalidate()
        },
    )
    Spacer(Modifier.height(4.dp))
    val groups = remember(points) { groupPlaces(points) }
    groups.forEachIndexed { i, g ->
        Column(Modifier.fillMaxWidth().padding(top = 13.dp)) {
            Text(
                g.name,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = placeColors[i % placeColors.size],
            )
            Text(
                "${g.days} 次到访 · ${g.photos} 张照片",
                fontSize = 11.sp,
                color = SubInk,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "📍 位置来自照片 EXIF，仅存本地 · 底图 © 高德地图",
        fontSize = 10.5.sp,
        color = SubInk,
    )
}
