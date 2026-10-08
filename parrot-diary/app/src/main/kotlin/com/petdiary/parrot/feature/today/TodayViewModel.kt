package com.petdiary.parrot.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petdiary.parrot.core.designsystem.BirdAccents
import com.petdiary.parrot.core.designsystem.petAccentColor
import com.petdiary.parrot.core.model.SpeciesCatalog
import com.petdiary.parrot.core.util.AgeFormatter
import com.petdiary.parrot.data.DiaryRepository
import com.petdiary.parrot.data.ExpenseRepository
import com.petdiary.parrot.data.FoodRepository
import com.petdiary.parrot.data.PetRepository
import com.petdiary.parrot.data.PlaceRepository
import com.petdiary.parrot.data.WeightRepository
import com.petdiary.parrot.data.local.DiaryEntryEntity
import com.petdiary.parrot.data.local.ExpenseLogEntity
import com.petdiary.parrot.data.local.FeedLogEntity
import com.petdiary.parrot.data.local.FoodEntity
import com.petdiary.parrot.data.local.PetEntity
import com.petdiary.parrot.data.local.WeightLogEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 当天时间线里的一条记录（称重/喂食/日记统一） */
data class DayRecordUi(
    val id: String,
    val kind: String, // WEIGHT / FEED / DIARY
    val title: String,
    val time: Long,
    val badge: String? = null,
)

data class TodayUi(
    val hasBird: Boolean,
    val name: String,
    val speciesName: String,
    val ageText: String?,
    val companionText: String?,
    val accent: androidx.compose.ui.graphics.Color,
    val initial: String,
    val latestGrams: Double?,
    val latestWeightText: String?,
    val recordCount: Int,
    val birds: List<PetEntity>,
    val selectedBirdId: String?,
    val weekDays: List<LocalDate>,
    val weekCounts: Map<LocalDate, Int>,
    val weekRecordCount: Int,
    val weekOffset: Int,
    val selectedDay: LocalDate,
    val dayRecords: List<DayRecordUi>,
    val monthActiveDays: Int,
    val monthRecords: Int,
    val monthCounts: Map<LocalDate, Int>,
    val yearCounts: Map<LocalDate, Int>,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    petRepository: PetRepository,
    private val weightRepository: WeightRepository,
    placeRepository: PlaceRepository,
    private val foodRepository: FoodRepository,
    private val diaryRepository: DiaryRepository,
    private val expenseRepository: ExpenseRepository,
) : ViewModel() {

    init {
        viewModelScope.launch {
            foodRepository.ensureBuiltinImported()
            placeRepository.ensureHomePlace()
        }
    }

    val expenseCategories: List<Pair<String, String>> = ExpenseRepository.CATEGORIES

    fun addExpense(category: String, amount: Double, note: String?) {
        viewModelScope.launch { expenseRepository.add(selectedBird.value?.id, category, amount, note) }
    }

    /** 发故事（朋友圈式）：文字/标签/照片任意组合，纯文字也可 */
    fun postStory(text: String, tags: List<String>, photoUris: List<android.net.Uri>) {
        val pet = selectedBird.value ?: return
        viewModelScope.launch { diaryRepository.createEntry(pet.id, text, tags, photoUris) }
    }

    val birds: StateFlow<List<PetEntity>> = petRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val selectedBirdId = MutableStateFlow<String?>(null)

    val selectedBird: StateFlow<PetEntity?> = combine(birds, selectedBirdId) { list, sel ->
        list.firstOrNull { it.id == sel } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val weights: StateFlow<List<WeightLogEntity>> = selectedBird
        .flatMapLatest { pet ->
            pet?.let { weightRepository.observeFor(it.id) } ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val feeds: StateFlow<List<FeedLogEntity>> = selectedBird
        .flatMapLatest { pet ->
            pet?.let { foodRepository.observeFeedsFor(it.id) } ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val diaries: StateFlow<List<DiaryEntryEntity>> = selectedBird
        .flatMapLatest { pet ->
            pet?.let { diaryRepository.observeForPet(pet.id) } ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 英雄卡背景：该鸟最新一张日记照片（无照片时为空，退回纯色渐变） */
    val heroPhotoPath: StateFlow<String?> = selectedBird
        .flatMapLatest { pet ->
            pet?.let { diaryRepository.observeMediaForPet(it.id) } ?: flowOf(emptyList())
        }
        .map { list ->
            list.maxByOrNull { it.takenAt ?: it.createdAt }?.path
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectBird(id: String) {
        selectedBirdId.value = id
    }

    fun recordWeight(grams: Double) {
        val pet = selectedBird.value ?: return
        viewModelScope.launch { weightRepository.record(pet.id, grams) }
    }

    private val allFoods: StateFlow<List<FoodEntity>> = foodRepository.observeAllFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 该鸟标记为 ❤️ 最爱的食物 (foodId, 名称)（时间线最爱徽章用） */
    private val favoriteFoods: StateFlow<List<Pair<String, String>>> = selectedBird
        .flatMapLatest { pet ->
            if (pet == null) flowOf(emptyList())
            else foodRepository.observePrefs(pet.id).combine(foodRepository.observeAllFoods()) { prefs, foods ->
                val names = foods.associate { it.id to it.name }
                prefs.filter { it.level == "LOVE" }.mapNotNull { p -> names[p.foodId]?.let { p.foodId to it } }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── 周历状态：选中日 + 周偏移 ──
    private val selectedDay = MutableStateFlow(LocalDate.now())
    private val weekOffset = MutableStateFlow(0)

    fun selectDay(d: LocalDate) {
        selectedDay.value = d
    }

    fun shiftWeek(delta: Int) {
        weekOffset.value += delta
    }

    fun backToToday() {
        weekOffset.value = 0
        selectedDay.value = LocalDate.now()
    }

    /** 该鸟全部记录（称重/喂食/日记）统一为时间线条目 */
    private val unifiedRecords: StateFlow<List<DayRecordUi>> = combine(
        weights,
        feeds,
        diaries,
        allFoods,
        selectedBird,
    ) { ws, fs, ds, foods, pet ->
        val name = pet?.name.orEmpty()
        val favIds = favoriteFoods.value.map { it.first }.toSet()
        val foodName = { id: String? -> foods.firstOrNull { it.id == id }?.name ?: "口粮" }
        buildList {
            ws.forEach { w -> add(DayRecordUi(w.id, "WEIGHT", "$name 称重 ${w.grams} g", w.weighedAt)) }
            fs.forEach { f ->
                add(
                    DayRecordUi(
                        f.id,
                        "FEED",
                        "$name 吃了 ${foodName(f.foodId)}",
                        f.fedAt,
                        if (f.foodId != null && f.foodId in favIds) "最爱" else null,
                    ),
                )
            }
            ds.forEach { d ->
                add(DayRecordUi(d.id, "DIARY", d.text.ifBlank { "$name 的日记" }, d.happenedAt))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun todayUi(today: LocalDate): StateFlow<TodayUi> = combine(
        unifiedRecords,
        selectedDay,
        weekOffset,
        birds,
        selectedBird,
    ) { records: List<DayRecordUi>, day: LocalDate, offset: Int, allBirds: List<PetEntity>, pet: PetEntity? ->
        val zone = ZoneId.systemDefault()
        fun dayOf(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        val monday = today.with(java.time.DayOfWeek.MONDAY).plusWeeks(offset.toLong())
        val week = (0..6L).map { monday.plusDays(it) }
        val weekSet = week.toSet()
        val weekRecs = records.filter { dayOf(it.time) in weekSet }.sortedByDescending { it.time }
        val weekCounts = weekRecs.groupingBy { dayOf(it.time) }.eachCount()
        val monthRecs = records.filter { dayOf(it.time).let { d -> d.year == today.year && d.month == today.month } }
        val weightsForPet = records.filter { it.kind == "WEIGHT" }
        TodayUi(
            hasBird = pet != null,
            name = pet?.name.orEmpty(),
            speciesName = pet?.speciesName.orEmpty(),
            ageText = pet?.let { AgeFormatter.formatAge(it.birthDateEpochDay, today.toEpochDay()) },
            companionText = pet?.let { AgeFormatter.formatCompanion(it.gotchaDateEpochDay, it.birthDateEpochDay, today.toEpochDay()) },
            accent = pet?.let(::petAccentColor) ?: BirdAccents[0],
            initial = pet?.name?.take(1).orEmpty(),
            latestGrams = weights.value.maxByOrNull { it.weighedAt }?.grams,
            latestWeightText = weights.value.maxByOrNull { it.weighedAt }?.let { "${it.grams} g" },
            recordCount = weights.value.size,
            birds = allBirds,
            selectedBirdId = pet?.id,
            weekDays = week,
            weekCounts = weekCounts,
            weekRecordCount = weekRecs.size,
            weekOffset = offset,
            selectedDay = day,
            dayRecords = records.filter { dayOf(it.time) == day }.sortedByDescending { it.time },
            monthActiveDays = monthRecs.groupingBy { dayOf(it.time) }.eachCount().size,
            monthRecords = monthRecs.size,
            monthCounts = monthRecs.groupingBy { dayOf(it.time) }.eachCount(),
            yearCounts = records.filter { dayOf(it.time).year == today.year }.groupingBy { dayOf(it.time) }.eachCount(),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TodayUi(false, "", "", null, null, BirdAccents[0], "", null, null, 0, emptyList(), null, weekDaysOf(LocalDate.now()), emptyMap(), 0, 0, LocalDate.now(), emptyList(), 0, 0, emptyMap(), emptyMap()),
    )

    private fun weekDaysOf(today: LocalDate): List<LocalDate> {
        val monday = today.with(java.time.DayOfWeek.MONDAY)
        return (0..6L).map { monday.plusDays(it) }
    }
}
