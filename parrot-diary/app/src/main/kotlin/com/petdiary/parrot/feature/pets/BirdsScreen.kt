package com.petdiary.parrot.feature.pets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.petdiary.parrot.core.designsystem.BirdAccents
import com.petdiary.parrot.data.local.PetEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirdsScreen(
    onBirdClick: (String) -> Unit,
    onAddClick: () -> Unit,
    vm: BirdsViewModel = hiltViewModel(),
) {
    val birds by vm.birds.collectAsState()
    val appearance by vm.appearance.collectAsState()
    val today = todayEpochDay()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的鸟") },
                actions = {
                    IconButton(onClick = vm::cycleAppearance) {
                        Icon(
                            imageVector = if (appearance == "DARK") Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = "切换外观",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "添加一只鸟")
            }
        },
    ) { padding ->
        if (birds.isEmpty()) {
            EmptyBirds(padding, onAddClick)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(birds, key = { it.id }) { pet ->
                    BirdRow(pet, today) { onBirdClick(pet.id) }
                }
            }
        }
    }
}

@Composable
private fun BirdRow(pet: PetEntity, today: Long, onClick: () -> Unit) {
    val ui = pet.toRowUi(today)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ui.accent.copy(alpha = 0.18f), CircleShape)
                    .border(2.dp, ui.accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (pet.avatarPath != null) {
                    AsyncImage(
                        model = java.io.File(pet.avatarPath),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(text = pet.name.take(1), fontWeight = FontWeight.Bold, color = ui.accent)
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 13.dp)) {
                Text(pet.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    listOfNotNull(
                        ui.speciesName,
                        ui.ageText,
                        ui.genderText.takeIf { it != "未知" || pet.gender == "U" }?.let { "性别：$it" },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyBirds(padding: PaddingValues, onAddClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(88.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("🦜", fontSize = MaterialTheme.typography.displaySmall.fontSize)
        }
        Spacer(Modifier.height(18.dp))
        Text("还没有鸟档案", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "点右下角 + 添加第一只鸟\n只需名字和品种，30 秒建好",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        Spacer(Modifier.height(18.dp))
        Text(
            "＋ 添加一只鸟",
            modifier = Modifier
                .clickable(onClick = onAddClick)
                .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large)
                .padding(horizontal = 20.dp, vertical = 11.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun accentColor(index: Int): Color = BirdAccents[index.coerceIn(BirdAccents.indices)]
