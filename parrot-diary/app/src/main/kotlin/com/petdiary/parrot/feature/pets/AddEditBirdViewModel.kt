package com.petdiary.parrot.feature.pets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petdiary.parrot.data.PetRepository
import com.petdiary.parrot.data.local.PersonalityPresetEntity
import com.petdiary.parrot.data.local.SpeciesEntity
import com.petdiary.parrot.data.local.PetEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 建鸟/编辑鸟表单状态（字段对照 APP设计文档.md §5） */
data class BirdFormState(
    val isEdit: Boolean = false,
    val name: String = "",
    val speciesId: String = "",
    val speciesName: String = "",
    val morph: String = "",
    val legBand: String = "",
    val source: String = "",
    val gender: String = "U",
    val refWeightMinG: Double? = null,
    val refWeightMaxG: Double? = null,
    val accentColor: Int? = null,
    val birthEpochDay: Long? = null,
    val gotchaEpochDay: Long? = null,
    val tags: Set<String> = emptySet(),
    val accentIndex: Int = 0,
    val notes: String = "",
    val loaded: Boolean = true,   // 编辑模式：档案加载完成前不可保存
) {
    val canSave: Boolean get() = name.isNotBlank() && speciesId.isNotBlank() && loaded
}

@HiltViewModel
class AddEditBirdViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val petRepository: PetRepository,
) : ViewModel() {

    private val petId: String? = savedStateHandle.get<String>("petId")

    private val _form = MutableStateFlow(BirdFormState(isEdit = petId != null, loaded = petId == null))
    val form: StateFlow<BirdFormState> = _form.asStateFlow()

    init {
        petId?.let { id ->
            viewModelScope.launch {
                petRepository.getById(id)?.let { pet -> _form.value = pet.toFormState() }
            }
        }
    }

    fun update(transform: (BirdFormState) -> BirdFormState) = _form.update(transform)

    /** 品种库（预置 + 自定义，落库可增删） */
    val species: StateFlow<List<SpeciesEntity>> = petRepository.observeSpecies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 个性标签预设池（落库可增删） */
    val presets: StateFlow<List<PersonalityPresetEntity>> = petRepository.observePresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addSpecies(name: String, weightMinG: Double?, weightMaxG: Double?, onAdded: (String) -> Unit) {
        viewModelScope.launch { onAdded(petRepository.addCustomSpecies(name, weightMinG, weightMaxG)) }
    }

    /** onResult=false 表示有鸟正在使用该品种 */
    fun deleteSpecies(id: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(petRepository.deleteSpecies(id)) }
    }

    fun addPreset(tag: String) {
        viewModelScope.launch { petRepository.addPreset(tag) }
    }

    fun deletePreset(tag: String) {
        viewModelScope.launch { petRepository.deletePreset(tag) }
    }

    fun save(onDone: () -> Unit) {
        val f = _form.value
        if (!f.canSave) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val tagsJson = if (f.tags.isEmpty()) null else f.tags.joinToString(separator = "\",\"", prefix = "[\"", postfix = "\"]")
            if (f.isEdit && petId != null) {
                val old = petRepository.getById(petId) ?: return@launch
                petRepository.update(
                    old.copy(
                        name = f.name.trim(),
                        speciesId = f.speciesId,
                        speciesName = f.speciesName,
                        refWeightMinG = f.refWeightMinG,
                        refWeightMaxG = f.refWeightMaxG,
                        accentColor = f.accentColor,
                        morph = f.morph.ifBlank { null },
                        legBand = f.legBand.ifBlank { null },
                        source = f.source.ifBlank { null },
                        gender = f.gender,
                        birthDateEpochDay = f.birthEpochDay,
                        gotchaDateEpochDay = f.gotchaEpochDay,
                        personalityTagsJson = tagsJson,
                        accentIndex = f.accentIndex,
                        notes = f.notes.ifBlank { null },
                        updatedAt = now,
                    ),
                )
            } else {
                petRepository.insert(
                    PetEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        name = f.name.trim(),
                        speciesId = f.speciesId,
                        speciesName = f.speciesName,
                        refWeightMinG = f.refWeightMinG,
                        refWeightMaxG = f.refWeightMaxG,
                        accentColor = f.accentColor,
                        morph = f.morph.ifBlank { null },
                        legBand = f.legBand.ifBlank { null },
                        source = f.source.ifBlank { null },
                        gender = f.gender,
                        birthDateEpochDay = f.birthEpochDay,
                        gotchaDateEpochDay = f.gotchaEpochDay,
                        personalityTagsJson = tagsJson,
                        accentIndex = f.accentIndex,
                        notes = f.notes.ifBlank { null },
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }
            onDone()
        }
    }
}

private fun PetEntity.toFormState() = BirdFormState(
    isEdit = true,
    name = name,
    speciesId = speciesId,
    speciesName = speciesName.orEmpty(),
    refWeightMinG = refWeightMinG,
    refWeightMaxG = refWeightMaxG,
    accentColor = accentColor,
    morph = morph.orEmpty(),
    legBand = legBand.orEmpty(),
    source = source.orEmpty(),
    gender = gender,
    birthEpochDay = birthDateEpochDay,
    gotchaEpochDay = gotchaDateEpochDay,
    tags = personalityTagsJson
        ?.trim('[', ']')
        ?.split("\",\"")
        ?.map { it.trim('"') }
        ?.filter { it.isNotBlank() }
        ?.toSet()
        ?: emptySet(),
    accentIndex = accentIndex,
    notes = notes.orEmpty(),
)
