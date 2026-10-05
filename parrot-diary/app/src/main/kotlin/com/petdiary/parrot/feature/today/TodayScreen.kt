package com.petdiary.parrot.feature.today

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel

import com.petdiary.parrot.core.designsystem.BirdAccents
import com.petdiary.parrot.data.local.ReminderEntity
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onBirdClick: (String) -> Unit,
    onAppearanceToggle: () -> Unit,
    appearanceDark: Boolean,
    onOpenBirdFoodLib: (String) -> Unit = {},
    vm: TodayViewModel = hiltViewModel(),
) {
    val today = LocalDate.now()
    val ui by vm.todayUi(today).collectAsState()
    val favoriteFoods by vm.favoriteFoods.collectAsState()
    val allFoods by vm.allFoods.collectAsState()
    val heroPhotoPath by vm.heroPhotoPath.collectAsState()
    var sheet by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current

    var pendingCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val u = pendingCameraUri
        pendingCameraUri = null
        if (ok && u != null) vm.saveCameraPhoto(u)
    }
    val cameraPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val f = java.io.File(context.cacheDir, "camera/${java.util.UUID.randomUUID()}.jpg").apply { parentFile?.mkdirs() }
            val uri = androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".fileprovider", f)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }
    fun shoot() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val f = java.io.File(context.cacheDir, "camera/${java.util.UUID.randomUUID()}.jpg").apply { parentFile?.mkdirs() }
            val uri = androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".fileprovider", f)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("今天") },
                actions = {
                    IconButton(onClick = onAppearanceToggle) {
                        Icon(
                            if (appearanceDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = "切换外观",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (ui.hasBird) {
                FloatingActionButton(onClick = { sheet = "menu" }) {
                    Icon(Icons.Default.Add, contentDescription = "记一笔")
                }
            }
        },
    ) { padding ->
        if (!ui.hasBird) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("去「鸟」页添加第一只鸟吧", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            ) {
                item { HeroCard(ui, ui.birds, heroPhotoPath, vm::selectBird) { onBirdClick(ui.selectedBirdId ?: return@HeroCard) } }
                item { Spacer(Modifier.height(10.dp)) }
                item {
                    CalendarCard(
                        ui = ui,
                        onPrev = { vm.shiftWeek(-1) },
                        onNext = { vm.shiftWeek(1) },
                        onToday = { vm.backToToday() },
                        onPickDay = { vm.selectDay(it) },
                    )
                }
                item {
                    val sel = ui.selectedDay
                    val isToday = sel == today
                    Text(
                        (if (isToday) "${sel.monthValue} 月 ${sel.dayOfMonth} 日 · 今天" else "${sel.monthValue} 月 ${sel.dayOfMonth} 日") +
                            " · ${ui.dayRecords.size} 条记录",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                }
                if (ui.dayRecords.isEmpty()) {
                    item {
                        Text(
                            "这一天还没有记录，点右下角 ＋ 记一笔",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                } else {
                    items(ui.dayRecords, key = { it.kind + it.id }) { r ->
                        RecordRow(r)
                    }
                }
                item { Spacer(Modifier.height(90.dp)) }
            }
        }
    }

    when (sheet) {
        "menu" -> ModalBottomSheet(onDismissRequest = { sheet = null }, sheetState = sheetState) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
            Text("记一笔 · ${ui.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp))
            SheetRow("⚖", "记体重", "默认上次 ${ui.latestWeightText ?: "—"}") { sheet = "weight" }
            SheetRow("🌾", "记喂食", "最爱食物优先") { sheet = "feed" }
            SheetRow("📷", "拍照", "随手拍进回忆 · 自动记录地点") { sheet = null; shoot() }
            SheetRow("✏️", "写日记", "文字 + 照片 + 心情标签") { sheet = "diary" }
            SheetRow("💰", "记开销", "花销随手记") { sheet = "expense" }
            Spacer(Modifier.height(14.dp))
            }
        }
        "weight" -> WeightSheet(
            birdName = ui.name,
            initialGrams = ui.latestGrams,
            onDismiss = { sheet = null },
            onSave = { grams ->
                vm.recordWeight(grams)
                sheet = null
            },
        )
        "feed" -> FeedSheet(
            birdName = ui.name,
            favorites = favoriteFoods,
            allFoods = allFoods,
            onDismiss = { sheet = null },
            onManageFood = {
                sheet = null
                ui.selectedBirdId?.let(onOpenBirdFoodLib)
            },
            onSave = { foodId, note ->
                vm.recordFeed(foodId, note)
                sheet = null
            },
        )
        "diary" -> DiarySheet(
            birdName = ui.name,
            onDismiss = { sheet = null },
            onSave = { text, tags, uris ->
                vm.saveDiary(text, tags, uris)
                sheet = null
            },
        )
        "expense" -> ExpenseSheet(
            categories = vm.expenseCategories,
            onDismiss = { sheet = null },
            onSave = { category, amount ->
                vm.addExpense(category, amount, null)
                sheet = null
            },
        )
    }
}

@Composable
private fun HeroCard(
    ui: TodayUi,
    birds: List<com.petdiary.parrot.data.local.PetEntity>,
    photoPath: String?,
    onSelect: (String) -> Unit,
    onOpenDetail: () -> Unit,
) {
    val bgPhoto = birds.firstOrNull { it.id == ui.selectedBirdId }?.avatarPath ?: photoPath
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(206.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF123A2C))
            .clickable(onClick = onOpenDetail),
    ) {
        // 第二层：这只鸟的头像照片（未设置时退回最新日记照片）
        if (bgPhoto != null) {
            AsyncImage(
                model = java.io.File(bgPhoto),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        // 顶层渐变：上深下透明再转深，保证头像与文字可读
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xFF123A2C).copy(alpha = 0.72f),
                        0.42f to Color.Transparent,
                        1f to Color(0xFF0D2B1F).copy(alpha = 0.94f),
                    ),
                ),
        )
        Row(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            birds.forEach { b ->
                val selected = b.id == ui.selectedBirdId
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (selected) Color.White else Color.White.copy(alpha = 0.18f), CircleShape)
                        .clickable { onSelect(b.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (b.avatarPath != null) {
                        AsyncImage(
                            model = java.io.File(b.avatarPath),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Text(b.name.take(1), fontWeight = FontWeight.Bold, color = Color(0xFF1D5C3E))
                    }
                }
            }
        }
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(ui.name, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(
                listOfNotNull(ui.speciesName, ui.ageText).joinToString(" · "),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassStat(ui.latestWeightText ?: "—", "最新体重")
                GlassStat(ui.companionText ?: "—", "陪伴时间")
                GlassStat("${ui.recordCount} 条", "记一笔数量")
            }
        }
    }
}

@Composable
private fun GlassStat(value: String, label: String) {
    Box(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Column {
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

/** 日历卡（原型 .cal-head/.week/.cal-month/.cal-year/.legend）：周/月/年 三模式切换 + 热力 */
@Composable
private fun CalendarCard(
    ui: TodayUi,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onPickDay: (LocalDate) -> Unit,
) {
    var mode by remember { mutableIntStateOf(0) } // 0=周 1=月 2=年
    val today = LocalDate.now()
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                when (mode) {
                    0 -> when {
                        ui.weekOffset == 0 -> "本周 · ${ui.weekRecordCount} 笔"
                        ui.weekOffset < 0 -> "${-ui.weekOffset} 周前 · ${ui.weekRecordCount} 笔"
                        else -> "${ui.weekOffset} 周后 · ${ui.weekRecordCount} 笔"
                    }
                    1 -> "${today.year} 年 ${today.monthValue} 月 · ${ui.monthCounts.values.sum()} 笔"
                    else -> "${today.year} 年 · ${ui.yearCounts.values.sum()} 笔"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (mode == 0) {
                    NavChip("‹", onPrev)
                    NavChip("今天", onToday)
                    NavChip("›", onNext)
                }
                NavChip(if (mode == 2) "周" else if (mode == 1) "年" else "月") { mode = (mode + 1) % 3 }
            }
        }
        Spacer(Modifier.height(10.dp))
        when (mode) {
            0 -> WeekStrip(ui, onPickDay)
            1 -> MonthGrid(today.withDayOfMonth(1), ui.monthCounts, ui.selectedDay, onPickDay)
            else -> YearHeatmap(today.year, ui.yearCounts)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(
                when (mode) {
                    0 -> "本月 ${ui.monthActiveDays} 天 · ${ui.monthRecords} 笔"
                    1 -> "本月 ${ui.monthCounts.values.count { it > 0 }} 天 · ${ui.monthCounts.values.sum()} 笔"
                    else -> "全年 ${ui.yearCounts.values.count { it > 0 }} 天"
                },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("少", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.primaryContainer,
                    Color(0xFFBFE0CD),
                    MaterialTheme.colorScheme.primary,
                ).forEach { c ->
                    Box(Modifier.size(9.dp).background(c, CircleShape))
                }
                Text("多", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** 周条（原型 .week）：七格热力 + 今日描边 + 点选切换日期 */
@Composable
private fun WeekStrip(ui: TodayUi, onPickDay: (LocalDate) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ui.weekDays.forEach { d ->
            val c = ui.weekCounts[d] ?: 0
            val isToday = d == LocalDate.now()
            val isSel = d == ui.selectedDay
            val bg = heatColor(c)
            val content = if (c >= 3) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(bg, RoundedCornerShape(13.dp))
                    .then(
                        when {
                            isSel -> Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(13.dp))
                            isToday -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(13.dp))
                            else -> Modifier
                        },
                    )
                    .clickable { onPickDay(d) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${d.dayOfMonth}",
                        fontSize = 13.sp,
                        fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = content,
                    )
                    Text(
                        d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.CHINESE),
                        fontSize = 9.5.sp,
                        color = content.copy(alpha = 0.65f),
                    )
                }
            }
        }
    }
}

/** 热力色阶（少 → 多） */
@Composable
private fun heatColor(c: Int) = when {
    c >= 3 -> MaterialTheme.colorScheme.primary
    c == 2 -> Color(0xFFBFE0CD)
    c == 1 -> MaterialTheme.colorScheme.primaryContainer
    else -> MaterialTheme.colorScheme.surfaceVariant
}

/** 月历（原型 .cal-month/.mgrid）：整月逐日热力，点格切到那天 */
@Composable
private fun MonthGrid(
    month: LocalDate,
    counts: Map<LocalDate, Int>,
    selected: LocalDate,
    onPick: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { wd ->
                Text(
                    wd,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
        val lead = month.dayOfWeek.value - 1 // 周一=1 → 月首前导空格数
        val daysInMonth = month.lengthOfMonth()
        val rows = (lead + daysInMonth + 6) / 7
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (c in 0 until 7) {
                    val dayNum = r * 7 + c - lead + 1
                    if (dayNum in 1..daysInMonth) {
                        val d = month.withDayOfMonth(dayNum)
                        val cnt = counts[d] ?: 0
                        val isToday = d == today
                        val isSel = d == selected
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(heatColor(cnt), RoundedCornerShape(9.dp))
                                .then(
                                    when {
                                        isSel -> Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(9.dp))
                                        isToday -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(9.dp))
                                        else -> Modifier
                                    },
                                )
                                .clickable { onPick(d) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "$dayNum",
                                fontSize = 12.sp,
                                fontWeight = if (cnt > 0) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (cnt >= 3) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** 年热力图（原型 .cal-year/.ygrid）：12 行月份 × 31 天，GitHub 贡献图式 */
@Composable
private fun YearHeatmap(year: Int, counts: Map<LocalDate, Int>) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        for (m in 1..12) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${m}月",
                    fontSize = 8.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(26.dp),
                )
                val dim = java.time.YearMonth.of(year, m).lengthOfMonth()
                for (d in 1..31) {
                    val cnt = if (d <= dim) counts[LocalDate.of(year, m, d)] ?: 0 else -1
                    val bg = when {
                        cnt < 0 -> Color.Transparent
                        cnt >= 4 -> MaterialTheme.colorScheme.primary
                        cnt >= 2 -> Color(0xFFBFE0CD)
                        cnt == 1 -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(10.dp)
                            .background(bg, RoundedCornerShape(2.dp)),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavChip(label: String, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

/** 当天时间线行：称重/喂食/日记统一（喂食带最爱徽章） */
@Composable
private fun RecordRow(r: com.petdiary.parrot.feature.today.DayRecordUi) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                when (r.kind) {
                    "WEIGHT" -> "⚖"
                    "FEED" -> "🌾"
                    else -> "✏️"
                },
                fontSize = 15.sp,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(r.title, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            Text(
                java.time.Instant.ofEpochMilli(r.time).atZone(java.time.ZoneId.systemDefault())
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (r.badge != null) {
            Text(
                r.badge,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

/** 记开销表单（原型 FAB 五项之一） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseSheet(
    categories: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onSave: (String, Double) -> Unit,
) {
    var cat by remember { mutableStateOf(categories.firstOrNull()?.first ?: "OTHER") }
    var amountText by remember { mutableStateOf("") }
    val amount = amountText.toDoubleOrNull()
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text("记一笔 · 开销", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp))
        Text("分类", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(bottom = 12.dp)) {
            categories.forEach { (code, label) ->
                FilterChip(selected = cat == code, onClick = { cat = code }, label = { Text(label) })
            }
        }
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
            singleLine = true,
            label = { Text("金额（元）") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onSave(cat, amount ?: 0.0) },
            enabled = (amount ?: 0.0) > 0,
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 14.dp),
        ) {
            Text("✓ 保存", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiarySheet(
    birdName: String,
    onDismiss: () -> Unit,
    onSave: (String, List<String>, List<android.net.Uri>) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf(setOf<String>()) }
    var uris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(6)) { result ->
        if (result.isNotEmpty()) uris = result
    }
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text("$birdName · 写日记", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("今天的小${birdName.take(1)}…") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )
        Text(
            "从相册选择照片（最多 6 张，自动读取拍摄地点）",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = uris.isNotEmpty(), onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, label = { Text("选择照片 ${if (uris.isNotEmpty()) "(${uris.size})" else ""}") })
        }
        Text("心情标签", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("换羽", "洗澡", "放风", "精神好", "精神一般", "萎靡").forEach { tag ->
                FilterChip(selected = tag in tags, onClick = { tags = if (tag in tags) tags - tag else tags + tag }, label = { Text(tag) })
            }
        }
        Button(
            onClick = { onSave(text, tags.toList(), uris); text = ""; uris = emptyList() },
            enabled = text.isNotBlank() || uris.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 14.dp),
        ) {
            Text("✓ 保存", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightSheet(birdName: String, initialGrams: Double?, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var grams by remember { mutableStateOf(initialGrams ?: 0.0) }
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text("$birdName · 记体重", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp))
        Text(
            "%.1f".format(grams) + " g",
            fontSize = 52.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Button(onClick = { grams = (grams - 0.5).coerceAtLeast(0.1) }, enabled = grams > 0.6) { Text("−", fontSize = 22.sp) }
            Spacer(Modifier.size(18.dp))
            Button(onClick = { grams = grams + 0.5 }) { Text("＋", fontSize = 22.sp) }
        }
        Button(
            onClick = { onSave((grams * 10).toInt() / 10.0) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = grams > 0,
        ) {
            Text("✓ 保存", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SheetRow(ico: String, title: String, sub: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(ico, fontSize = 20.sp)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedSheet(
    birdName: String,
    favorites: List<Pair<String, String>>,
    allFoods: List<com.petdiary.parrot.data.local.FoodEntity>,
    onDismiss: () -> Unit,
    onManageFood: () -> Unit,
    onSave: (String?, String?) -> Unit,
) {
    var selectedFoodId by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text("$birdName · 记喂食", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp))
        Text(
            "最爱食物优先 · 点选后保存",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        Text("最爱", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 10.dp)) {
            favorites.forEach { (id, name) ->
                FilterChip(selected = selectedFoodId == id, onClick = { selectedFoodId = id }, label = { Text(name) })
            }
            if (favorites.isEmpty()) {
                Text(
                    "还没有最爱标记 · 去食物库标一个",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onManageFood),
                )
            }
        }
        Text("其他常吃", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 10.dp)) {
            // 禁吃食物不进入快捷选择（§6.3）
            allFoods.filter { f -> favorites.none { it.first == f.id } && f.safety != "DANGER" }.take(5).forEach { f ->
                FilterChip(selected = selectedFoodId == f.id, onClick = { selectedFoodId = f.id }, label = { Text(f.name) })
            }
        }
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("备注（可选）") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { onSave(selectedFoodId, note.ifBlank { null }) },
            enabled = selectedFoodId != null || note.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 12.dp),
        ) {
            Text("✓ 保存", fontWeight = FontWeight.Bold)
        }
        Text(
            "库里没有？管理食物库 ›",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onManageFood)
                .padding(top = 10.dp, bottom = 14.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
