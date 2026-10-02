package com.petdiary.parrot.feature.memories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 回忆 · 纸面令牌（APP设计文档.md §7：浅纸绿底 StoryTeller 纸面） */
val PaperBg = Color(0xFFF2F6F2)
val GreenInk = Color(0xFF2F8F5B)
val GreenInkSoft = Color(0xFFDCEEE2)
val SubInk = Color(0xFF77807A)
val PaperInk = Color(0xFF1B1C1E)

/** 回忆区固定浅纸主题：不随全局深色翻转（纸面永远是纸面） */
@Composable
fun PaperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = PaperBg,
            onBackground = PaperInk,
            surface = Color.White,
            onSurface = PaperInk,
            surfaceVariant = Color(0xFFE7EEE7),
            onSurfaceVariant = Color(0xFF5E685F),
        ),
        typography = MaterialTheme.typography,
        content = content,
    )
}

@Composable
fun FilterChipAlt(selected: Boolean, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (selected) GreenInk else Color(0xFFE9EFEA),
                RoundedCornerShape(999.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp),
    ) {
        Text(
            label,
            fontSize = 12.5.sp,
            color = if (selected) Color.White else Color(0xFF3E4A44),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
fun EmptyHint(text: String) {
    Text(text, fontSize = 13.sp, color = SubInk, modifier = Modifier.padding(vertical = 14.dp), lineHeight = 20.sp)
}
