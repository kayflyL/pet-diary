package com.petdiary.parrot.feature.pets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petdiary.parrot.core.designsystem.BirdAccents
import com.petdiary.parrot.core.util.AgeFormatter
import com.petdiary.parrot.data.PetRepository
import com.petdiary.parrot.data.SettingsRepository
import com.petdiary.parrot.data.local.PetEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class BirdsViewModel @Inject constructor(
    petRepository: PetRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val birds: StateFlow<List<PetEntity>> = petRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val appearance: StateFlow<String> = settingsRepository.appearance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "SYSTEM")

    private val order = listOf("SYSTEM", "LIGHT", "DARK")

    fun cycleAppearance() {
        viewModelScope.launch {
            settingsRepository.setAppearance(order[(order.indexOf(appearance.value) + 1) % order.size])
        }
    }
}

/** 列表行的展示派生数据 */
data class BirdRowUi(
    val id: String,
    val name: String,
    val speciesName: String,
    val ageText: String?,
    val genderText: String,
    val accent: androidx.compose.ui.graphics.Color,
)

fun PetEntity.toRowUi(todayEpochDay: Long): BirdRowUi = BirdRowUi(
    id = id,
    name = name,
    speciesName = com.petdiary.parrot.core.model.SpeciesCatalog.nameOf(speciesId),
    ageText = AgeFormatter.formatAge(birthDateEpochDay, todayEpochDay),
    genderText = AgeFormatter.genderLabel(gender),
    accent = BirdAccents[accentIndex.coerceIn(BirdAccents.indices)],
)

fun todayEpochDay(): Long = LocalDate.now().toEpochDay()
