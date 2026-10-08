package com.petdiary.parrot.core.designsystem

import androidx.compose.ui.graphics.Color

// v1.2 视觉规范（APP设计文档.md §9.1）：白色底 + 草木绿主色，深色成对
// 浅色
val PrimaryLight = Color(0xFF2F8F5B)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFE3F4EA)
val OnPrimaryContainerLight = Color(0xFF0F3D28)
val SecondaryLight = Color(0xFF52634F)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFD5E8D4)
val OnSecondaryContainerLight = Color(0xFF101F14)
val TertiaryLight = Color(0xFF3C6472)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFC0E9FA)
val OnTertiaryContainerLight = Color(0xFF001F29)
val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)
val BackgroundLight = Color(0xFFFFFFFF)
val OnBackgroundLight = Color(0xFF1B1C1E)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF1B1C1E)
val SurfaceVariantLight = Color(0xFFF0F1F0)
val OnSurfaceVariantLight = Color(0xFF43474A)
val OutlineLight = Color(0xFFD9DADD)
val OutlineVariantLight = Color(0xFFEAEAE8)

// 深色
val PrimaryDark = Color(0xFF7FD3A2)
val OnPrimaryDark = Color(0xFF0E2A1C)
val PrimaryContainerDark = Color(0xFF1F3A2B)
val OnPrimaryContainerDark = Color(0xFFA7E3C1)
val SecondaryDark = Color(0xFFB9CCB9)
val OnSecondaryDark = Color(0xFF243427)
val SecondaryContainerDark = Color(0xFF3A4B3C)
val OnSecondaryContainerDark = Color(0xFFD5E8D4)
val TertiaryDark = Color(0xFFA4CDDE)
val OnTertiaryDark = Color(0xFF003544)
val TertiaryContainerDark = Color(0xFF234C5B)
val OnTertiaryContainerDark = Color(0xFFC0E9FA)
val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)
val BackgroundDark = Color(0xFF111316)
val OnBackgroundDark = Color(0xFFE7E9EA)
val SurfaceDark = Color(0xFF111316)
val OnSurfaceDark = Color(0xFFE7E9EA)
val SurfaceVariantDark = Color(0xFF22262A)
val OnSurfaceVariantDark = Color(0xFF9BA0A6)
/** 鸟的强调色：自定义 ARGB 优先，否则按 accentIndex 查色板 */
fun petAccentColor(pet: com.petdiary.parrot.data.local.PetEntity): androidx.compose.ui.graphics.Color =
    pet.accentColor?.let { androidx.compose.ui.graphics.Color(it) }
        ?: BirdAccents[pet.accentIndex.coerceIn(BirdAccents.indices)]

val OutlineDark = Color(0xFF3A3E43)
val OutlineVariantDark = Color(0xFF26292D)

// 每鸟强调色（APP设计文档.md §9.1）：只管"这是谁"，主色管交互
val BirdAccents = listOf(
    Color(0xFFE8A93D), // 玄凤黄
    Color(0xFF6FA84C), // 虎皮绿
    Color(0xFFE8785A), // 牡丹珊瑚
    Color(0xFF6C8FB5), // 灰鹦蓝
    Color(0xFF9B7FC4), // 紫罗兰
    Color(0xFFE58BA9), // 樱花粉
)

/** 强调色全色板（编辑档案可自选，存 ARGB 到 pets.accentColor） */
val AccentPalette = BirdAccents + listOf(
    Color(0xFF3FA08C), // 青鸭绿
    Color(0xFF5A6FC4), // 靛蓝
    Color(0xFFC48A5A), // 焦糖
    Color(0xFF7C9A3F), // 苔藓
    Color(0xFFC48A9E), // 玫瑰金
    Color(0xFF6B7076), // 石墨
)
