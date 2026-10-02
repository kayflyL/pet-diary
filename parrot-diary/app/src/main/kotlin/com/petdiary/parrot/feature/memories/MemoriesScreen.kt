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
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
fun MemoriesScreen(vm: MemoriesViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    var seg by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()
    var viewer by remember { mutableStateOf<ViewerState?>(null) }
    val placeCount = ui.geotagged
        .groupBy { it.placeName ?: "%.3f,%.3f".format(it.lat ?: 0.0, it.lng ?: 0.0) }
        .size

    PaperTheme {
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
                            color = if (seg == i) PaperInk else TabIdle,
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
                // 参与 Compose 布局（View 实际仍按 656px 绘制，压住列表），
                // 待找到互操作安全的方案再补（§9 的 180ms 淡入位移暂缓）
                when (seg) {
                    0 -> StoryWall(ui.stories, scrollState)
                    1 -> PhotoMap(ui.geotagged) { viewer = it }
                    else -> PhotoWall(ui.photos) { viewer = it }
                }
            }
        }
        viewer?.let { state ->
            PhotoViewer(state = state, onDismiss = { viewer = null })
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

/** 按地点聚合的列表行（设计文档 §7：大字地点列表，无卡片框） */
private data class PlaceGroup(val name: String, val cover: String, val photos: Int, val days: Int)

private fun groupPlaces(points: List<GeoPhoto>): List<PlaceGroup> =
    points
        .groupBy { placeKeyOf(it) }
        .map { (_, list) ->
            PlaceGroup(
                name = list.first().photo.placeName ?: "%.4f, %.4f".format(list.first().lat, list.first().lng),
                cover = list.first().photo.path,
                photos = list.size,
                days = list.map { it.photo.date }.distinct().size,
            )
        }
        .sortedByDescending { it.photos }

/** 照片裁圆 + 白描边 → 圆形照片钉图标 */
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

/** 聚合气泡：主照片圆 + 右上角墨色计数徽章（缩小时相邻照片合成一点，放大自动散开） */
private fun clusterDrawable(ctx: Context, firstPhotoPath: String, count: Int, sizeDp: Float): Drawable? {
    val density = ctx.resources.displayMetrics.density
    val sizePx = (sizeDp * density).toInt().coerceIn(40, 200)
    val base = circlePhotoDrawable(ctx, firstPhotoPath, sizeDp.toFloat()) as? BitmapDrawable
    val baseBmp = base?.bitmap ?: return null
    val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
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
    return BitmapDrawable(ctx.resources, out)
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
private data class ViewerState(val photos: List<PhotoUi>, val startIndex: Int)

/** 全屏大图查看：左右切换同组照片，点任意处关闭 */
@Composable
private fun PhotoViewer(state: ViewerState, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val pagerState = rememberPagerState(initialPage = state.startIndex) { state.photos.size }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF0F1210))
                .clickable(onClick = onDismiss),
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { i ->
                AsyncImage(
                    model = java.io.File(state.photos[i].path),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
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
                        SimpleDateFormat("M 月 d 日", Locale.CHINESE).format(Date(p.date.toEpochDay() * 86400000L)) +
                            " · ${pagerState.currentPage + 1}/${state.photos.size}",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 3.dp),
                    )
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

/** 照片地图：高德底图 + 照片钉 + 缩放聚合 + 点击看同地点大图（2026-10-03 定案，替代旧"行迹"轨迹线） */
@Composable
private fun PhotoMap(geotagged: List<PhotoUi>, onOpenViewer: (ViewerState) -> Unit) {
    val points = geotagged.mapNotNull { p ->
        val lat = p.lat ?: return@mapNotNull null
        val lng = p.lng ?: return@mapNotNull null
        GeoPhoto(lat, lng, p)
    }
    if (points.isEmpty()) {
        Text(
            "拍照会自动记录拍摄地点（EXIF）——有了第一张带位置的照片，这里就会亮起照片地图",
            style = MaterialTheme.typography.bodyMedium,
            color = SubInk,
            modifier = Modifier.padding(vertical = 14.dp),
        )
        return
    }
    val ctx = LocalContext.current
    var clusterTick by remember { mutableIntStateOf(0) }
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
            val density = ctx.resources.displayMetrics.density
            val nodes = clusterNodes(map, points, 46f * density)
            nodes.forEach { node ->
                map.overlays.add(
                    Marker(map).apply {
                        position = GeoPoint(node.lat, node.lng)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        if (node.photos.size == 1) {
                            circlePhotoDrawable(ctx, node.photos[0].photo.path, 40f)?.let { icon = it }
                        } else {
                            clusterDrawable(ctx, node.photos[0].photo.path, node.photos.size, 46f)?.let { icon = it }
                        }
                        setOnMarkerClickListener { _, _ ->
                            val distinctPlaces = node.photos.map { placeKeyOf(it) }.distinct()
                            val openViewer: () -> Unit = {
                                if (node.photos.size == 1) {
                                    // 单张：看大图，左右滑切换同一地点的全部照片
                                    val key = placeKeyOf(node.photos[0])
                                    val group = points.filter { placeKeyOf(it) == key }
                                    val start = group.indexOfFirst { it.photo.id == node.photos[0].photo.id }.coerceAtLeast(0)
                                    onOpenViewer(ViewerState(group.map { it.photo }, start))
                                } else {
                                    onOpenViewer(ViewerState(node.photos.map { it.photo }, 0))
                                }
                            }
                            if (node.photos.size == 1 || distinctPlaces.size == 1 || map.zoomLevelDouble >= 17.0) {
                                // 单点、或整组同一地点：直接看大图（同地点照片永远聚合在一起，
                                // 等缩放拆开是等不到的）；缩放很深仍聚合时也直接看
                                openViewer()
                            } else {
                                // 聚合点混着多个地点：放大散开再挑
                                map.controller.animateTo(GeoPoint(node.lat, node.lng))
                                map.controller.zoomIn()
                            }
                            true
                        }
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
        "📍 照片按拍摄位置（EXIF）落点，仅存本地 · 缩小地图自动聚合 · 底图 © 高德地图",
        fontSize = 10.5.sp,
        color = SubInk,
    )
}
