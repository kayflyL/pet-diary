package com.petdiary.parrot.data

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.petdiary.parrot.data.local.DiaryDao
import com.petdiary.parrot.data.local.DiaryEntryEntity
import com.petdiary.parrot.data.local.DiaryMediaEntity
import com.petdiary.parrot.data.local.GeoMediaRow
import com.petdiary.parrot.data.local.PlaceMediaRow
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * 日记仓库（APP设计文档.md §7）：
 * 照片复制进私有 media/ 目录（卸载即删 → 由备份体系覆盖）；
 * 位置来自照片 EXIF（不申请定位权限）。
 */
@Singleton
class DiaryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: DiaryDao,
) {
    private val mediaDir: File get() = File(context.filesDir, "media").apply { mkdirs() }

    fun observeAllEntries(): Flow<List<DiaryEntryEntity>> = dao.observeAll()
    fun observeForPet(petId: String): Flow<List<DiaryEntryEntity>> = dao.observeForPet(petId)
    fun observeMediaForPet(petId: String): Flow<List<DiaryMediaEntity>> = dao.observeMediaForPet(petId)
    fun observeAllMedia(): Flow<List<DiaryMediaEntity>> = dao.observeAllMedia()
    fun observeGeotagged(): Flow<List<DiaryMediaEntity>> = dao.observeGeotagged()
    fun observeGeotaggedNamed(): Flow<List<GeoMediaRow>> = dao.observeGeoNamed()
    fun observePlaceMedia(): Flow<List<PlaceMediaRow>> = dao.observePlaceMedia()

    /** 写日记：文本 + 标签 + 可选多图（自动复制并读取 EXIF 位置） */
    suspend fun createEntry(
        petId: String,
        text: String,
        tags: List<String> = emptyList(),
        photoUris: List<Uri> = emptyList(),
        at: Long = System.currentTimeMillis(),
    ): String {
        val entryId = UUID.randomUUID().toString()
        val tagsJson = if (tags.isEmpty()) null else tags.joinToString(separator = "\",\"", prefix = "[\"", postfix = "\"]")
        dao.insertEntry(
            DiaryEntryEntity(id = entryId, petId = petId, text = text, tags = tagsJson, happenedAt = at, createdAt = now(), updatedAt = now()),
        )
        photoUris.forEachIndexed { i, uri ->
            val saved = copyIntoMedia(uri) ?: return@forEachIndexed
            val (lat, lng) = readGps(saved)
            dao.insertMedia(
                listOf(
                    DiaryMediaEntity(
                        id = UUID.randomUUID().toString(),
                        entryId = entryId,
                        path = saved.absolutePath,
                        type = "IMAGE",
                        sort = i,
                        lat = lat,
                        lng = lng,
                        takenAt = null,
                        placeId = null,
                        createdAt = now(),
                    ),
                ),
            )
        }
        return entryId
    }

    /** 编辑日记：更新文字与心情标签（照片集暂不在此改） */
    suspend fun updateEntryTextTags(entryId: String, text: String, tags: List<String>) {
        val entry = dao.getEntryById(entryId) ?: return
        val tagsJson = if (tags.isEmpty()) null else tags.joinToString(separator = "\",\"", prefix = "[\"", postfix = "\"]")
        dao.updateEntry(entry.copy(text = text.trim(), tags = tagsJson, updatedAt = now()))
    }

    /** 相机直拍：cache 里的文件移入 media 目录并建日记 */
    suspend fun saveCameraPhoto(petId: String, cameraUri: Uri, text: String = "", at: Long = System.currentTimeMillis()): String {
        val entryId = UUID.randomUUID().toString()
        val saved = copyIntoMedia(cameraUri) ?: return ""
        val (lat, lng) = readGps(saved)
        dao.insertEntry(
            DiaryEntryEntity(id = entryId, petId = petId, text = text, tags = null, happenedAt = at, createdAt = now(), updatedAt = now()),
        )
        dao.insertMedia(
            listOf(
                DiaryMediaEntity(
                    id = UUID.randomUUID().toString(),
                    entryId = entryId,
                    path = saved.absolutePath,
                    type = "IMAGE",
                    sort = 0,
                    lat = lat,
                    lng = lng,
                    takenAt = null,
                    placeId = null,
                    createdAt = now(),
                ),
            ),
        )
        return entryId
    }

    /** 相机文件 → media 目录（先读 EXIF 再落盘） */
    private suspend fun copyIntoMedia(uri: Uri): File? {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val dest = File(mediaDir, "${UUID.randomUUID()}.jpg")
        dest.writeBytes(bytes)
        return dest
    }

    private fun readGps(file: File): Pair<Double?, Double?> = runCatching {
        val exif = ExifInterface(file.absolutePath)
        exif.latLong?.let { it[0].toDouble() to it[1].toDouble() } ?: (null to null)
    }.getOrDefault(null to null)

    private fun now() = System.currentTimeMillis()
}
