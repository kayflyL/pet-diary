package com.petdiary.parrot.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petdiary.parrot.core.designsystem.BirdAccents
import com.petdiary.parrot.core.model.SpeciesCatalog
import com.petdiary.parrot.core.util.AgeFormatter
import com.petdiary.parrot.data.PetRepository
import com.petdiary.parrot.data.PlaceRepository
import com.petdiary.parrot.data.WeightRepository
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DayDots(val date: LocalDate, val count: Int)

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
    val selectedDay: LocalDate,
    val selectedDayRecords: List<WeightLogEntity>,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    petRepository: PetRepository,
    private val weightRepository: WeightRepository,
    placeRepository: PlaceRepository,
) : ViewModel() {

    init {
        viewModelScope.launch { placeRepository.ensureHomePlace() }
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

    fun selectBird(id: String) {
        selectedBirdId.value = id
    }

    fun recordWeight(grams: Double) {
        val pet = selectedBird.value ?: return
        viewModelScope.launch { weightRepository.record(pet.id, grams) }
    }

    fun todayUi(today: LocalDate): StateFlow<TodayUi> =
        combine(selectedBird, birds, weights) { pet, allBirds, weights ->
            val zone = ZoneId.systemDefault()
            fun dayOf(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
            val monday = today.with(java.time.DayOfWeek.MONDAY)
            val week = (0..6L).map { monday.plusDays(it) }
            val counts = weights.groupingBy { dayOf(it.weighedAt) }.eachCount()
            val selected = today // 选中日恒为今天（简化：历史回看在 M3.5 加）
            val todayRecords = weights.filter { dayOf(it.weighedAt) == selected }.sortedByDescending { it.weighedAt }
            TodayUi(
                hasBird = pet != null,
                name = pet?.name.orEmpty(),
                speciesName = pet?.let { SpeciesCatalog.nameOf(it.speciesId) }.orEmpty(),
                ageText = pet?.let { AgeFormatter.formatAge(it.birthDateEpochDay, today.toEpochDay()) },
                companionText = pet?.let { AgeFormatter.formatCompanion(it.gotchaDateEpochDay, it.birthDateEpochDay, today.toEpochDay()) },
                accent = BirdAccents[(pet?.accentIndex ?: 0).coerceIn(BirdAccents.indices)],
                initial = pet?.name?.take(1).orEmpty(),
                latestGrams = weights.maxByOrNull { it.weighedAt }?.grams,
                latestWeightText = weights.maxByOrNull { it.weighedAt }?.let { "${it.grams} g" },
                recordCount = weights.size,
                birds = allBirds,
                selectedBirdId = pet?.id,
                weekDays = week,
                weekCounts = counts,
                selectedDay = selected,
                selectedDayRecords = todayRecords,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUi(false, "", "", null, null, BirdAccents[0], "", null, null, 0, emptyList(), null, weekDaysOf(today), emptyMap(), today, emptyList()))

    private fun weekDaysOf(today: LocalDate): List<LocalDate> {
        val monday = today.with(java.time.DayOfWeek.MONDAY)
        return (0..6L).map { monday.plusDays(it) }
    }
}
