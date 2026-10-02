package com.petdiary.parrot.feature.memories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petdiary.parrot.data.DiaryRepository
import com.petdiary.parrot.data.PetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class StoryUi(
    val date: LocalDate,
    val petNames: List<String>,
    val entryCount: Int,
    val photoCount: Int,
    val texts: List<String>,
    val firstPhotoPath: String?,
)

data class PhotoUi(
    val id: String,
    val path: String,
    val petName: String,
    val date: LocalDate,
    val lat: Double?,
    val lng: Double?,
)

data class MemoriesUi(
    val stories: List<StoryUi>,
    val photos: List<PhotoUi>,
    val geotagged: List<PhotoUi>,
)

@HiltViewModel
class MemoriesViewModel @Inject constructor(
    diaryRepository: DiaryRepository,
    petRepository: PetRepository,
) : ViewModel() {

    private val zone = ZoneId.systemDefault()

    val ui: StateFlow<MemoriesUi> = combine(
        diaryRepository.observeAllEntries(),
        diaryRepository.observeAllMedia(),
        petRepository.observeActive(),
    ) { entries, media, pets ->
        val nameById = pets.associate { it.id to it.name }
        val mediaByEntry = media.groupBy { it.entryId }

        val stories = entries
            .sortedByDescending { it.happenedAt }
            .groupBy { Instant.ofEpochMilli(it.happenedAt).atZone(zone).toLocalDate() }
            .map { (date, list) ->
                val ids = list.map { it.id }
                val dayMedia = media.filter { it.entryId in ids }
                StoryUi(
                    date = date,
                    petNames = list.mapNotNull { nameById[it.petId] }.distinct(),
                    entryCount = list.size,
                    photoCount = dayMedia.size,
                    texts = list.map { it.text },
                    firstPhotoPath = dayMedia.firstOrNull()?.path,
                )
            }

        val photos = media
            .sortedByDescending { it.createdAt }
            .map { m ->
                val entry = entries.firstOrNull { it.id == m.entryId }
                PhotoUi(
                    id = m.id,
                    path = m.path,
                    petName = nameById[entry?.petId].orEmpty(),
                    date = Instant.ofEpochMilli(m.createdAt).atZone(zone).toLocalDate(),
                    lat = m.lat,
                    lng = m.lng,
                )
            }

        MemoriesUi(stories = stories, photos = photos, geotagged = photos.filter { it.lat != null && it.lng != null })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MemoriesUi(emptyList(), emptyList(), emptyList()))
}
