package com.petdiary.parrot.feature.memories

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.roundToInt

/** 地点大字列表的轮换色（设计文档 §7：绿/蓝/琥珀交替） */
private val placeColors = listOf(Color(0xFF2F8F5B), Color(0xFF4A7FB5), Color(0xFFB07A16))

/** 巨型文字 Tab 未选中态的浅灰（原型 .mt 色） */
private val TabIdle = Color(0xFFC7C9CC)

/** 回忆 · 纸面（APP设计文档.md §7，StoryTeller 结构）· 数据来自 MemoriesViewModel */
@Composable
fun MemoriesScreen(onOpenPlace: (String) -> Unit = {}, vm: MemoriesViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    var seg by remember { mutableIntStateOf(0) }
    var mapFs by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    var viewer by remember { mutableStateOf<ViewerState?>(null) }
    val placeCount = ui.geotagged
        .groupBy { it.placeName ?: "%.3f,%.3f".format(it.lat ?: 0.0, it.lng ?: 0.0) }
        .size

    PaperTheme {
        Box(Modifier.fillMaxSize()) {
            Scaffold { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(PaperBg)
                        .verticalScroll(scrollState)
                        .padding(18.dp),
                ) {
                    Text(
                        "MEMORIES",
                        fontSize = 10.5.sp,
                        letterSpacing = 2.5.sp,
                        color = GreenInk,
                        fontWeight = FontWeight.Bold,
                    )
                    // 巨型文字 Tab：大号文字即页签（StoryTeller DNA ①）
                    Row(Modifier.padding(top = 4.dp)) {
                        listOf("故事", "地图", "照片").forEachIndexed { i, label ->
                            Text(
                                label,
                                fontSize = 27.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp,
                                color = if (seg == i && !mapFs) PaperInk else if (mapFs) TabIdle else TabIdle,
                                modifier = Modifier
                                    .padding(end = 13.dp)
                                    .clickable { seg = i },
                            )
                        }
                    }
                    Text(
                        "${ui.stories.size} 个故事 · ${ui.photos.size} 张照片 · $placeCount 个地方",
                        fontSize = 12.sp,
                        color = SubInk,
                        modifier = Modifier.padding(top = 4.dp),
                    )

                    QuoteBlock()

                    // 分段切换暂不做过渡动画：AnimatedContent 会让互操作 MapView 以 0 高
                    // 参与 Compose 布局（View 实际仍按原尺寸绘制，压住列表），待互操作安全方案
                    when (seg) {
                        0 -> StoryWall(ui.stories, scrollState)
                        1 -> if (!mapFs) {
                            PhotoMap(
                                geotagged = ui.geotagged,
                                fullscreen = false,
                                onOpenViewer = { viewer = it },
                                onOpenPlace = onOpenPlace,
                                onToggleFullscreen = { mapFs = true },
                            )
                        }
                        else -> PhotoWall(ui.photos) { viewer = it }
                    }
                }
            }
            // 全屏地图（原型 ⛶）：覆盖整个内容区含底部导航
            if (mapFs) {
                PhotoMap(
                    geotagged = ui.geotagged,
                    fullscreen = true,
                    onOpenViewer = { viewer = it },
                    onOpenPlace = onOpenPlace,
                    onToggleFullscreen = { mapFs = false },
                )
            }
            viewer?.let { state ->
                PhotoViewer(state = state, onDismiss = { viewer = null })
            }
        }
    }
}

/** 引言块：StoryTeller 情感设计的点睛（原型 .mquote，纯排版无框线） */
@Composable
private fun QuoteBlock() {
    Column(Modifier.padding(top = 14.dp, bottom = 16.dp)) {
        Text(
            "把照片重新变回事件",
            fontSize = 16.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.3).sp,
            color = PaperInk,
        )
        Text(
            "每一次称重、每一顿饭、每一张照片，都是你们共同经历的情感元数据。地图不只是定位，是讲故事时重踏的足迹。",
            fontSize = 12.5.sp,
            color = SubInk,
            lineHeight = 21.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            "—— 设计理念致敬 Nokia Storyteller",
            fontSize = 10.5.sp,
            color = SubInk.copy(alpha = 0.75f),
            modifier = Modifier.padding(top = 5.dp),
        )
    }
}

/** 故事墙：左侧月份竖轨 + 双列瀑布流（StoryTeller DNA ②）；竖轨可点、随滚动联动高亮 */
@Composable
private fun StoryWall(stories: List<StoryUi>, scrollState: ScrollState) {
    if (stories.isEmpty()) {
        Text(
            "还没有故事。在「今天」写一条日记，同一天的照片和日记会自动打包成一个故事。",
            style = MaterialTheme.typography.bodyMedium,
            color = SubInk,
        )
        return
    }
    val months = remember(stories) {
        stories.map { it.date.withDayOfMonth(1) }.distinct().sortedDescending()
    }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // 各月份首块在滚动内容里的 y 偏移（用于竖轨联动与点击定位）
    val monthTops = remember { mutableStateMapOf<LocalDate, Int>() }
    var contentRootY by remember { mutableStateOf(0f) }
    val activeMonth by remember(months) {
        derivedStateOf {
            monthTops.entries
                .filter { it.value <= scrollState.value + 40 }
                .maxByOrNull { it.value }?.key ?: months.first()
        }
    }

    Row(Modifier.fillMaxWidth().onGloballyPositioned { contentRootY = it.positionInRoot().y }) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.width(34.dp),
        ) {
            months.forEach { m ->
                val on = m == activeMonth
                Text(
                    "${m.monthValue}月",
                    fontSize = 9.5.sp,
                    fontWeight = if (on) FontWeight.ExtraBold else FontWeight.Normal,
                    color = if (on) GreenInk else SubInk,
                    modifier = Modifier.clickable {
                        monthTops[m]?.let { top ->
                            scope.launch { scrollState.animateScrollTo((top - 8).coerceAtLeast(0)) }
                        }
                    },
                )
            }
        }
        // 双列瀑布流：按全局序号循环高度比，两列交错
        val ratios = listOf(1f, 1.28f, 0.9f, 1.15f)
        val columns = listOf(
            stories.filterIndexed { i, _ -> i % 2 == 0 },
            stories.filterIndexed { i, _ -> i % 2 == 1 },
        )
        val firstOfMonth = remember(stories) {
            stories.groupBy { it.date.withDayOfMonth(1) }.mapValues { (_, v) -> v.first().date }
        }
        Row(Modifier.weight(1f).padding(start = 10.dp)) {
            columns.forEach { col ->
                Column(Modifier.weight(1f)) {
                    col.forEach { story ->
                        val ratio = ratios[(stories.indexOf(story)) % ratios.size]
                        StoryTile(
                            story = story,
                            ratio = ratio,
                            onAnchor = {
                                if (firstOfMonth[story.date.withDayOfMonth(1)] == story.date) {
                                    monthTops[story.date.withDayOfMonth(1)] =
                                        (it.positionInRoot().y - contentRootY).roundToInt()
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/** 瀑布流故事瓦片：照片全出血 + 压字（原型 .stile） */
@Composable
private fun StoryTile(story: StoryUi, ratio: Float, onAnchor: (androidx.compose.ui.layout.LayoutCoordinates) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(16.dp))
            .background(GreenInkSoft)
            .onGloballyPositioned { onAnchor(it) },
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
                Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(Color(0xFFBFE3CE), Color(0xFF5FA97E)))),
            )
        }
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x990F2317))))
                .padding(11.dp),
        ) {
            Text(
                when {
                    story.texts.isNotEmpty() -> story.texts.first().take(14)
                    else -> "${story.date.monthValue} 月 ${story.date.dayOfMonth} 日"
                },
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                "${story.date.monthValue} 月 ${story.date.dayOfMonth} 日 · ${story.photoCount} 张 · ${story.entryCount} 条",
                fontSize = 10.5.sp,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 1.dp),
            )
        }
    }
}

/** 照片墙：按月分组的方片网格，点任意一张全屏看大图（左右滑看全部） */
@Composable
private fun PhotoWall(photos: List<PhotoUi>, onOpenViewer: (ViewerState) -> Unit) {
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
                            .background(GreenInkSoft)
                            .clickable {
                                val start = photos.indexOfFirst { it.id == p.id }.coerceAtLeast(0)
                                onOpenViewer(ViewerState(photos, start))
                            },
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

/** 地图上的一张带位置照片 */
private data class GeoPhoto(val lat: Double, val lng: Double, val photo: PhotoUi)

/** 地点归组键：有名用名（可手动归并），无名按坐标就近聚合（约百米粒度） */
private fun placeKeyOf(g: GeoPhoto): String = g.photo.placeName ?: "%.3f,%.3f".format(g.lat, g.lng)

/** 按地点聚合的列表行（设计文档 §7：大字地点列表，无卡片框）；key=归组键，named=有地名可进详情 */
private data class PlaceGroup(val key: String, val name: String, val cover: String, val photos: Int, val days: Int)

private fun groupPlaces(points: List<GeoPhoto>): List<PlaceGroup> =
    points
        .groupBy { placeKeyOf(it) }
        .map { (key, list) ->
            PlaceGroup(
                key = key,
                name = list.first().photo.placeName ?: "%.4f, %.4f".format(list.first().lat, list.first().lng),
                cover = list.first().photo.path,
                photos = list.size,
                days = list.map { it.photo.date }.distinct().size,
            )
        }
        .sortedByDescending { it.photos }

/** 照片裁圆 + 白描边 → 圆形照片钉图标；label 非空时在圆下画地名小签（全屏模式） */
private fun circlePhotoDrawable(ctx: Context, path: String, sizeDp: Float, label: String? = null): Drawable? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val density = ctx.resources.displayMetrics.density
    val sizePx = (sizeDp * density).toInt().coerceIn(32, 160)
    val labPx = if (label != null) (18f * density).toInt() else 0
    val sample = maxOf(1, minOf(bounds.outWidth, bounds.outHeight) / (sizePx * 2))
    val bmp = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    val side = minOf(bmp.width, bmp.height)
    val sq = Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
    val ring = 2.5f * density
    val radius = sizePx / 2f - ring
    val out = Bitmap.createBitmap(sizePx, sizePx + labPx, Bitmap.Config.ARGB_8888)
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
    if (label != null) drawLabelChip(canvas, label, sizePx / 2f, sizePx + 2f * density, density)
    return BitmapDrawable(ctx.resources, out)
}

/** 聚合气泡：主照片圆 + 右上角墨色计数徽章（缩小时相邻照片合成一点，放大自动散开） */
private fun clusterDrawable(ctx: Context, firstPhotoPath: String, count: Int, sizeDp: Float, label: String? = null): Drawable? {
    val density = ctx.resources.displayMetrics.density
    val sizePx = (sizeDp * density).toInt().coerceIn(40, 200)
    val base = circlePhotoDrawable(ctx, firstPhotoPath, sizeDp.toFloat()) as? BitmapDrawable
    val baseBmp = base?.bitmap ?: return null
    val labPx = if (label != null) (18f * density).toInt() else 0
    val out = Bitmap.createBitmap(sizePx, sizePx + labPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    canvas.drawBitmap(baseBmp, 0f, 0f, null)
    val badgeR = 9f * density
    val cx = sizePx - badgeR
    val cy = badgeR
    canvas.drawCircle(cx, cy, badgeR, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF22312A.toInt() })
    canvas.drawCircle(
        cx,
        cy,
        badgeR,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f * density
            color = android.graphics.Color.WHITE
        },
    )
    canvas.drawText(
        count.toString(),
        cx,
        cy + 3.8f * density,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 11f * density
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        },
    )
    if (label != null) drawLabelChip(canvas, label, sizePx / 2f, sizePx + 2f * density, density)
    return BitmapDrawable(ctx.resources, out)
}

/** 圆点下的地名小签（原型 .ptag：墨底白字圆角） */
private fun drawLabelChip(canvas: Canvas, text: String, cx: Float, top: Float, density: Float) {
    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 10f * density
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    val chipW = tp.measureText(text) + 12f * density
    val chipH = 14f * density
    canvas.drawRoundRect(
        RectF(cx - chipW / 2, top, cx + chipW / 2, top + chipH),
        6f * density,
        6f * density,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCC16241C.toInt() },
    )
    canvas.drawText(text, cx, top + chipH * 0.72f, tp)
}

/** 屏幕距离聚类：相邻 < clusterPx 的照片合成一个节点（每次缩放后按当前投影重算） */
private fun clusterNodes(map: MapView, points: List<GeoPhoto>, clusterPx: Float): List<MapNode> {
    val proj = map.projection
    val pts = points.map { g -> g to proj.toPixels(GeoPoint(g.lat, g.lng), null) }
    val used = BooleanArray(pts.size)
    val nodes = mutableListOf<MapNode>()
    for (i in pts.indices) {
        if (used[i]) continue
        used[i] = true
        val ax = pts[i].second.x
        val ay = pts[i].second.y.toFloat()
        val group = mutableListOf(pts[i].first)
        for (j in i + 1 until pts.size) {
            if (used[j]) continue
            val dx = pts[j].second.x - ax
            val dy = pts[j].second.y - ay
            if (dx * dx + dy * dy <= clusterPx * clusterPx) {
                used[j] = true
                group.add(pts[j].first)
            }
        }
        nodes.add(MapNode(group, group.map { it.lat }.average(), group.map { it.lng }.average()))
    }
    return nodes
}

/** 地图节点：1 张=单照片钉，>1 张=聚合气泡 */
private data class MapNode(val photos: List<GeoPhoto>, val lat: Double, val lng: Double)

/** 大图查看器的状态：某组照片 + 起始张 */
internal data class ViewerState(val photos: List<PhotoUi>, val startIndex: Int)

/** 全屏大图查看：左右切换同组照片；点击图片缩放（2.1x），放大后拖拽平移/双指捏合 */
@Composable
internal fun PhotoViewer(state: ViewerState, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val pagerState = rememberPagerState(initialPage = state.startIndex) { state.photos.size }
        var zoom by remember { mutableFloatStateOf(1f) }
        var tx by remember { mutableFloatStateOf(0f) }
        var ty by remember { mutableFloatStateOf(0f) }
        Box(Modifier.fillMaxSize().background(Color(0xFF0F1210))) {
            HorizontalPager(state = pagerState, userScrollEnabled = zoom == 1f, modifier = Modifier.fillMaxSize()) { i ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = java.io.File(state.photos[i].path),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = zoom
                                scaleY = zoom
                                translationX = if (pagerState.currentPage == i) tx else 0f
                                translationY = if (pagerState.currentPage == i) ty else 0f
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = {
                                    zoom = if (zoom > 1f) 1f else 2.1f
                                    if (zoom == 1f) { tx = 0f; ty = 0f }
                                })
                            }
                            .pointerInput(zoom > 1f) {
                                if (zoom > 1f) {
                                    detectTransformGestures { _, pan, gestureZoom, _ ->
                                        val nz = (zoom * gestureZoom).coerceIn(1f, 4f)
                                        zoom = nz
                                        tx += pan.x
                                        ty += pan.y
                                        if (nz == 1f) { tx = 0f; ty = 0f }
                                    }
                                }
                            },
                    )
                }
            }
            state.photos.getOrNull(pagerState.currentPage)?.let { p ->
                Column(
                    Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(16.dp),
                ) {
                    Text(
                        p.placeName ?: "拍摄地点",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        SimpleDateFormat("M 月 d 日 HH:mm", Locale.CHINESE).format(Date(p.takenAt ?: p.date.toEpochDay() * 86400000L)) +
                            " · ${pagerState.currentPage + 1}/${state.photos.size}",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                    if (p.lat != null && p.lng != null) {
                        Text(
                            "📍 " + "%.4f, %.4f".format(p.lat, p.lng),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
            Text(
                "✕",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(14.dp)
                    .clickable(onClick = onDismiss)
                    .padding(4.dp),
            )
        }
    }
}

/** 快览卡状态：某组照片 + 当前张（点预览图进全屏查看器）——移植自用户 photo_gallery 的聚合弹窗 */
private data class QuickView(val photos: List<PhotoUi>, val index: Int)

/**
 * 照片地图（原型 v3.4 对齐）：高德底图 + 照片钉 + 缩放聚合分散 + 地图内快览浮层
 * + 时光滑轨（全屏）+ ⛶ 全屏模式 + fitBounds 自适应。
 */
@Composable
private fun PhotoMap(
    geotagged: List<PhotoUi>,
    fullscreen: Boolean,
    onOpenViewer: (ViewerState) -> Unit,
    onOpenPlace: (String) -> Unit,
    onToggleFullscreen: () -> Unit,
) {
    val all = geotagged.mapNotNull { p ->
        val lat = p.lat ?: return@mapNotNull null
        val lng = p.lng ?: return@mapNotNull null
        GeoPhoto(lat, lng, p)
    }
    if (all.isEmpty()) {
        Text(
            "拍照会自动记录拍摄地点（EXIF）——有了第一张带位置的照片，这里就会亮起照片地图",
            style = MaterialTheme.typography.bodyMedium,
            color = SubInk,
            modifier = Modifier.padding(vertical = 14.dp),
        )
        return
    }
    // 时光滑轨：按拍摄时间升序，拖动看照片一点点亮起（points = 当前亮起的子集）
    val ordered = remember(all) { all.sortedBy { it.photo.takenAt ?: (it.photo.date.toEpochDay() * 86400000L) } }
    var shown by remember { mutableIntStateOf(ordered.size) }
    val points = remember(ordered, shown) { ordered.take(shown) }
    var quick by remember { mutableStateOf<QuickView?>(null) }
    val ctx = LocalContext.current
    var clusterTick by remember { mutableIntStateOf(0) }
    val mapRadiusPx = 18f * ctx.resources.displayMetrics.density
    val placeCount = remember(points) { points.groupBy { placeKeyOf(it) }.size }
    val mapView = remember {
        org.osmdroid.config.Configuration.getInstance().apply {
            userAgentValue = ctx.packageName
            osmdroidBasePath = java.io.File(ctx.filesDir, "osmdroid")
            osmdroidTileCache = java.io.File(ctx.cacheDir, "osmdroid")
        }
        MapView(ctx).apply {
            setTileSource(amapTileSource())
            setMultiTouchControls(true)
            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
            // 互操作层不响应 Compose 的 clip；圆角与边界裁剪只能在 View 自身做
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, mapRadiusPx)
                }
            }
            addMapListener(
                object : MapListener {
                    override fun onScroll(event: ScrollEvent?): Boolean = false
                    override fun onZoom(event: ZoomEvent?): Boolean {
                        clusterTick++
                        return false
                    }
                },
            )
        }
    }
    // 读一下 tick：缩放发生时触发重组，让下面的 update 重新聚类
    val tick = clusterTick
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (fullscreen) Modifier.fillMaxSize() else Modifier.height(300.dp)),
    ) {
        // 地图区：快览浮层/缩放钮/提示签都"长在地图上"（对齐原型）
        Box(
            Modifier
                .fillMaxWidth()
                .then(if (fullscreen) Modifier.weight(1f) else Modifier.height(300.dp)),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { c ->
                    mapView.apply {
                        // fitBounds：按可见照片范围自适应缩放（对齐 photo_gallery）
                        val bb = BoundingBox.fromGeoPoints(points.map { GeoPoint(it.lat, it.lng) })
                        controller.setCenter(GeoPoint((bb.latSouth + bb.latNorth) / 2, (bb.lonWest + bb.lonEast) / 2))
                        post { zoomToBoundingBox(bb.increaseByScale(1.25f), false) }
                    }
                },
                update = { map ->
                    map.overlays.clear()
                    val density = ctx.resources.displayMetrics.density
                    val nodes = clusterNodes(map, points, 46f * density)
                    nodes.forEach { node ->
                        map.overlays.add(
                            Marker(map).apply {
                                position = GeoPoint(node.lat, node.lng)
                                // 全屏模式给圆点画地名签；混合了多个地点的聚合不标单一地名
                                val label = if (fullscreen && node.photos.map { placeKeyOf(it) }.distinct().size == 1) {
                                    node.photos[0].photo.placeName
                                } else null
                                val labDp = if (label != null) 18f else 0f
                                if (node.photos.size == 1) {
                                    circlePhotoDrawable(ctx, node.photos[0].photo.path, 40f, label)?.let { icon = it }
                                    setAnchor(Marker.ANCHOR_CENTER, 20f / (40f + labDp))
                                } else {
                                    clusterDrawable(ctx, node.photos[0].photo.path, node.photos.size, 46f, label)?.let { icon = it }
                                    setAnchor(Marker.ANCHOR_CENTER, 23f / (46f + labDp))
                                }
                                // 对齐原型：点任何钉/聚合都弹快览卡（photo_gallery zoomToBoundsOnClick:false）
                                setOnMarkerClickListener { _, _ ->
                                    if (node.photos.size == 1) {
                                        val key = placeKeyOf(node.photos[0])
                                        val group = points.filter { placeKeyOf(it) == key }
                                        val start = group.indexOfFirst { it.photo.id == node.photos[0].photo.id }.coerceAtLeast(0)
                                        quick = QuickView(group.map { it.photo }, start)
                                    } else {
                                        quick = QuickView(node.photos.map { it.photo }, 0)
                                    }
                                    true
                                }
                            },
                        )
                    }
                    map.invalidate()
                },
            )
            // ＋－ 缩放按钮（原型 .zbtns 左下）
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MapChip("＋") { mapView.controller.zoomIn() }
                MapChip("－") { mapView.controller.zoomOut() }
            }
            // 提示浮签（原型 .zoomnote 右下）
            Text(
                "双指缩放 · 放大后聚合散开成单张照片 · 拖动平移",
                fontSize = 10.sp,
                color = SubInk,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(Color(0xFFE7EEE7), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            if (fullscreen) {
                // ← 退出全屏（原型 .fs-back）
                MapChip("←", Modifier.align(Alignment.TopStart).padding(12.dp)) { onToggleFullscreen() }
                // 统计浮牌（原型 .fs-stats）
                Text(
                    "📍 ${points.size} 张有位置信息 · $placeCount 个地方",
                    fontSize = 10.5.sp,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(Color(0xB3101914), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            } else {
                // ⛶ 进入全屏（原型 .fs-btn）
                MapChip("⛶", Modifier.align(Alignment.TopEnd).padding(10.dp)) { onToggleFullscreen() }
            }
            // 快览浮层（两段式第一段：预览 + 左右切换，点预览图进全屏）
            quick?.let { q ->
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(10.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xF2101412)),
                ) {
                    Box(Modifier.fillMaxWidth().height(170.dp)) {
                        AsyncImage(
                            model = java.io.File(q.photos[q.index].path),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onOpenViewer(ViewerState(q.photos, q.index)) },
                            contentScale = ContentScale.Fit,
                        )
                        QuickNavBtn(
                            "‹",
                            Modifier
                                .align(Alignment.CenterStart)
                                .padding(10.dp),
                        ) { quick = q.copy(index = (q.index - 1 + q.photos.size) % q.photos.size) }
                        QuickNavBtn(
                            "›",
                            Modifier
                                .align(Alignment.CenterEnd)
                                .padding(10.dp),
                        ) { quick = q.copy(index = (q.index + 1) % q.photos.size) }
                        Text(
                            "✕",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color(0x66000000), CircleShape)
                                .clickable { quick = null }
                                .padding(7.dp),
                        )
                    }
                    val p = q.photos[q.index]
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            (p.placeName ?: "拍摄地点") + " · " +
                                SimpleDateFormat("M 月 d 日 HH:mm", Locale.CHINESE).format(Date(p.takenAt ?: p.date.toEpochDay() * 86400000L)),
                            fontSize = 11.sp,
                            color = Color(0xFFA8B3AC),
                        )
                        Text("${q.index + 1} / ${q.photos.size}", fontSize = 11.sp, color = Color(0xFFA8B3AC))
                    }
                }
            }
        }
        // 时光滑轨面板（原型 .fs-slider：全屏时位于地图下方，不与地图重叠）
        if (fullscreen) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xD9101312))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    if (shown >= ordered.size) "📅 全部时间 · ${ordered.size} 张" else "📅 至 ${points.last().photo.date.monthValue} 月 · $shown 张",
                    fontSize = 11.sp,
                    color = Color(0xFFA8B3AC),
                    modifier = Modifier.padding(bottom = 2.dp),
                )
                Slider(
                    value = shown.toFloat(),
                    onValueChange = { shown = it.toInt().coerceIn(1, ordered.size) },
                    valueRange = 1f..ordered.size.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${ordered.first().photo.date.monthValue}月", fontSize = 9.sp, color = Color(0xFFA8B3AC))
                    Text("${ordered.last().photo.date.monthValue}月", fontSize = 9.sp, color = Color(0xFFA8B3AC))
                }
            }
        }
    }
    // 非全屏：地点大字列表 + 脚注（全屏模式按原型不显示列表）
    if (!fullscreen) {
        Spacer(Modifier.height(4.dp))
        val groups = remember(points) { groupPlaces(points) }
        groups.forEachIndexed { i, g ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 13.dp)
                    .clickable(enabled = g.key == g.name) { onOpenPlace(g.name) },
            ) {
                Text(
                    g.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = placeColors[i % placeColors.size],
                )
                Text(
                    "${g.days} 次到访 · ${g.photos} 张照片" + if (g.key == g.name) " · 点击进入 ›" else "",
                    fontSize = 11.sp,
                    color = SubInk,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "📍 照片按拍摄位置（EXIF）落点，仅存本地 · ⛶ 进全屏地图 · 底图 © 高德地图",
            fontSize = 10.5.sp,
            color = SubInk,
        )
    }
}

/** 地图浮层小圆钮（＋－/←，原型 .zbtn/.fs-back） */
@Composable
private fun MapChip(symbol: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        symbol,
        color = Color.White,
        fontSize = 14.sp,
        modifier = modifier
            .background(Color(0x99101914), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

/** 快览卡的圆形左右切换钮 */
@Composable
private fun QuickNavBtn(symbol: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        symbol,
        color = Color.White,
        fontSize = 18.sp,
        modifier = modifier
            .background(Color(0x99000000), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    )
}
