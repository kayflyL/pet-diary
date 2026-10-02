package com.petdiary.parrot.feature.memories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CinemaBg = Color(0xFF0D1013)
private val CinemaCard = Color(0xFF16191D)
private val CinemaRoad = Color(0xFF1D2429)
private val CinemaInk = Color(0xFFEDEEEF)
private val CinemaSub = Color(0xFF8B9096)
private val CinemaGreen = Color(0xFF7FD3A2)

/**
 * 回忆 · 影院画布（APP设计文档.md §7，致敬 Nokia StoryTeller）
 * M5 接入真实数据；当前为视觉骨架。
 */
@Composable
fun MemoriesScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CinemaBg)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
    ) {
        Text(
            "MEMORIES",
            fontSize = 10.5.sp,
            letterSpacing = 2.5.sp,
            color = CinemaGreen,
            fontWeight = FontWeight.Bold,
        )
        Text("回忆", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = CinemaInk)
        Text(
            "5 个地点 · 103 张照片 · 行迹 42 km",
            fontSize = 12.sp,
            color = CinemaSub,
            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
        )

        MapSkeleton()

        Spacer(Modifier.height(18.dp))
        Text("故事 · M5", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CinemaInk)
        Text(
            "同一天同一地的照片与日记会自动打包成一个故事\n——比如「人民公园半日游 · 6 张照片」",
            fontSize = 12.5.sp,
            color = CinemaSub,
            modifier = Modifier.padding(top = 6.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text("照片 · M5", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CinemaInk)
        Text(
            "随手拍的照片会按月聚在这里，并可按地点回顾",
            fontSize = 12.5.sp,
            color = CinemaSub,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun MapSkeleton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(235.dp)
            .background(CinemaCard, RoundedCornerShape(20.dp)),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .align(Alignment.TopCenter)
                .background(CinemaRoad),
        )
        Box(
            Modifier
                .width(12.dp)
                .height(200.dp)
                .align(Alignment.CenterEnd)
                .background(CinemaRoad),
        )
        MapPin("🏠", "家 · 大本营", Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = 60.dp))
        MapPin("🌳", "人民公园 · M5", Modifier.align(Alignment.TopEnd).padding(end = 12.dp, top = 120.dp))
        Text(
            "行迹地图 · M5 上线（高德地图 + 照片定位聚合）",
            fontSize = 11.sp,
            color = CinemaSub,
            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
        )
    }
}

@Composable
private fun MapPin(
    emoji: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(CinemaInk, RoundedCornerShape(topStart = 13.dp, topEnd = 13.dp, bottomEnd = 13.dp, bottomStart = 3.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 12.sp)
        }
        Text(label, fontSize = 10.sp, color = CinemaSub, modifier = Modifier.padding(top = 3.dp))
    }
}
