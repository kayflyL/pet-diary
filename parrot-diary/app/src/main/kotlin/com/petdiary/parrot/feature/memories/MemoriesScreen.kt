package com.petdiary.parrot.feature.memories

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.petdiary.parrot.core.designsystem.BirdAccents
import java.time.Instant
import java.time.ZoneId
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

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

@Composable
private fun StoryList(stories: List<StoryUi>) {
    if (stories.isEmpty()) {
        EmptyHint("还没有故事。在「今天」写一条日记，同一天的照片和日记会自动打包成一个故事。")
        return
    }
    stories.forEach { s ->
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(modifier = Modifier.size(10.dp).background(GreenInk, CircleShape))
                Text(
                    "${s.date.monthValue} 月 ${s.date.dayOfMonth} 日 · " + s.petNames.joinToString("、"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (s.firstPhotoPath != null) {
                AsyncImage(
                    model = java.io.File(s.firstPhotoPath),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.6f).padding(top = 8.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
            }
            s.texts.take(2).forEach { t ->
                Text(t, fontSize = 12.5.sp, color = SubInk, modifier = Modifier.padding(top = 5.dp))
            }
            Text("${s.entryCount} 条日记 · ${s.photoCount} 张照片", fontSize = 11.sp, color = SubInk, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun PhotoWall(photos: List<PhotoUi>) {
    if (photos.isEmpty()) {
        EmptyHint("随手拍的照片会按月聚在这里")
        return
    }
    val byMonth = photos.groupBy { it.date.withDayOfMonth(1) }.toSortedMap(compareByDescending { it.year * 100 + it.monthValue })
    byMonth.forEach { (month, list) ->
        Text(
            "${month.year} 年 ${month.monthValue} 月 · ${list.size} 张",
            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SubInk,
            modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            list.take(3).forEach { p ->
                AsyncImage(
                    model = java.io.File(p.path),
                    contentDescription = null,
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
private fun TrailMap(geotagged: List<PhotoUi>) {
    val points = geotagged.mapNotNull { p ->
        val lat = p.lat ?: return@mapNotNull null
        val lng = p.lng ?: return@mapNotNull null
        Triple(lat, lng, p)
    }
    if (points.isEmpty()) {
        EmptyHint("拍照会自动记录拍摄地点（EXIF）——有了第一张带位置的照片，这里就会亮起足迹地图")
        return
    }
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(250.dp),
        factory = { c ->
            MapView(c).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(9.0)
                controller.setCenter(GeoPoint(points.first().first, points.first().second))
                points.forEach { (lat, lng, _) ->
                    overlays.add(
                        Marker(this).apply {
                            position = GeoPoint(lat, lng)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "拍摄地点"
                        },
                    )
                }
            }
        },
    )
    Spacer(Modifier.height(10.dp))
    points.take(5).forEach { (lat, lng, p) ->
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(34.dp).background(GreenInkSoft, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Text("📷", fontSize = 14.sp)
            }
            Column(Modifier.padding(start = 10.dp)) {
                Text("%.4f, %.4f".format(lat, lng), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                Text(p.date.let { "${it.year}-${it.monthValue}-${it.dayOfMonth}" }, fontSize = 10.5.sp, color = SubInk)
            }
        }
    }
}
