package com.petdiary.parrot.feature.pets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petdiary.parrot.core.designsystem.BirdAccents
import com.petdiary.parrot.core.model.SpeciesCatalog
import com.petdiary.parrot.core.util.AgeFormatter
import com.petdiary.parrot.core.util.DietStats
import com.petdiary.parrot.data.PetRepository
import com.petdiary.parrot.data.WeightRepository
import com.petdiary.parrot.data.local.FoodDao
import com.petdiary.parrot.data.local.FeedLogDao
import com.petdiary.parrot.data.local.PetEntity
import com.petdiary.parrot.data.local.PetFoodPrefDao
import com.petdiary.parrot.data.local.WeightLogEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class BirdDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    petRepository: PetRepository,
    weightRepository: WeightRepository,
    feedLogDao: FeedLogDao,
    foodDao: FoodDao,
    prefDao: PetFoodPrefDao,
) : ViewModel() {

    val petId: String = checkNotNull(savedStateHandle["petId"])

    val bird: StateFlow<PetEntity?> = petRepository.observeById(petId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val latestWeight: StateFlow<WeightLogEntity?> = weightRepository.observeLatest(petId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val weights: StateFlow<List<WeightLogEntity>> = weightRepository.observeFor(petId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recordCount: StateFlow<Int> = weightRepository.observeCount(petId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** 饮食卡片：最爱（手动标记）+ 最近 30 天实际构成（DietStats） */
    val diet: StateFlow<DietUi> = combine(
        feedLogDao.observeForPet(petId),
        foodDao.observeAll(),
        prefDao.observeForPet(petId),
    ) { feeds, foods, prefs ->
        val names = foods.associate { it.id to it.name }
        val favorites = prefs.filter { it.level == "LOVE" }.mapNotNull { names[it.foodId] }
        val shares = DietStats.compute(feeds.map { it.fedAt to it.foodId }, names, System.currentTimeMillis())
        DietUi(favorites = favorites, shares = shares)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DietUi(emptyList(), emptyList()))
}

data class DietUi(val favorites: List<String>, val shares: List<DietStats.FoodShare>)

/** 详情页展示派生数据 */
data class BirdDetailUi(
    val name: String,
    val speciesName: String,
    val genderText: String,
    val ageText: String?,
    val companionText: String?,
    val accent: androidx.compose.ui.graphics.Color,
    val initial: String,
    val tags: List<String>,
    val legBand: String?,
    val source: String?,
    val birthText: String?,
    val gotchaText: String?,
    val notes: String?,
    val latestWeightText: String?,
    val recordCount: Int,
)

fun detailUi(pet: PetEntity?, latest: WeightLogEntity?, count: Int, todayEpochDay: Long): BirdDetailUi? {
    pet ?: return null
    return BirdDetailUi(
        name = pet.name,
        speciesName = pet.speciesName.orEmpty(),
        genderText = AgeFormatter.genderLabel(pet.gender),
        ageText = AgeFormatter.formatAge(pet.birthDateEpochDay, todayEpochDay),
        companionText = AgeFormatter.formatCompanion(pet.gotchaDateEpochDay, pet.birthDateEpochDay, todayEpochDay),
        accent = com.petdiary.parrot.core.designsystem.petAccentColor(pet),
        initial = pet.name.take(1),
        tags = pet.personalityTagsJson
            ?.trim('[', ']')
            ?.split("\",\"")
            ?.map { it.trim('"') }
            ?.filter { it.isNotBlank() }
            ?: emptyList(),
        legBand = pet.legBand,
        source = pet.source,
        birthText = pet.birthDateEpochDay?.let { java.time.LocalDate.ofEpochDay(it).toString() },
        gotchaText = pet.gotchaDateEpochDay?.let { java.time.LocalDate.ofEpochDay(it).toString() },
        notes = pet.notes,
        latestWeightText = latest?.let { "${it.grams} g" },
        recordCount = count,
    )
}
