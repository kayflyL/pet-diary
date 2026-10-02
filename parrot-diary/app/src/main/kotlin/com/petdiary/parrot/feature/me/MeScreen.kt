package com.petdiary.parrot.feature.me

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MeScreen(
    appearance: String,
    onCycleAppearance: () -> Unit,
    onOpenReminders: () -> Unit = {},
    onOpenExpenses: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
    ) {
        Text("我的", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))

        SettingRow(Icons.Outlined.DarkMode, "外观", appearanceLabel(appearance) + " ›", onCycleAppearance)
        SettingRow(Icons.Outlined.Wallet, "花费统计", "›", onOpenExpenses)
        SettingRow(Icons.Outlined.CalendarMonth, "提醒管理", "M6 ›", onOpenReminders)
        SettingRow(Icons.Outlined.Eco, "食物库", "M4 ›")
        SettingRow(Icons.Outlined.Eco, "食物库", "M4 ›")
        Spacer(Modifier.height(20.dp))
        Text(
            "鹦鹉日记 v0.1.0 · 数据仅存储在你的手机上 🧡",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun appearanceLabel(v: String) = when (v) {
    "LIGHT" -> "浅色"
    "DARK" -> "深色"
    else -> "跟随系统"
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, trailing: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(21.dp))
        Spacer(Modifier.padding(end = 12.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(trailing, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
