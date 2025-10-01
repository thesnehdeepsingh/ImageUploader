package com.esfersoft.imageuploader.data.storage

import android.content.ContentResolver
import android.net.Uri
import com.esfersoft.imageuploader.domain.ImageMetadataEntity
import com.esfersoft.imageuploader.domain.UploadProgressEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

interface StorageRepository {
    fun uploadImage(uri: Uri, metadata: ImageMetadataEntity): Flow<UploadProgressEntity>
}

class MockStorageRepository(private val contentResolver: ContentResolver) : StorageRepository {
    override fun uploadImage(uri: Uri, metadata: ImageMetadataEntity): Flow<UploadProgressEntity> = flow {
        val totalBytes = contentResolver.openFileDescriptor(uri, "r")?.statSize ?: 500_000L
        var sent = 0L
        var progress = 0f
        while (progress < 1f) {
            delay(100)
            val chunk = Random.nextLong(10_000, 60_000)
            sent = (sent + chunk).coerceAtMost(totalBytes)
            progress = sent.toFloat() / totalBytes.toFloat()
            emit(UploadProgressEntity(uri, progress = progress.coerceIn(0f, 1f), bytesSent = sent, totalBytes = totalBytes, isCompleted = progress >= 1f))
        }
    }
}
