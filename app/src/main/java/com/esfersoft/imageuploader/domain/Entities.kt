package com.esfersoft.imageuploader.domain

import android.net.Uri

data class ImageEntity(
    val id: Long,
    val uri: Uri,
    val displayName: String?,
    val dateAddedEpochSeconds: Long,
    val sizeBytes: Long
)

enum class SelectionMode { SINGLE, MULTI }

data class SelectedImageEntity(
    val uri: Uri
)

data class ImageMetadataEntity(
    val caption: String = "",
    val tags: List<String> = emptyList()
)

data class UploadProgressEntity(
    val uri: Uri,
    val progress: Float, // 0f..1f
    val bytesSent: Long = 0L,
    val totalBytes: Long = 0L,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null
)

sealed class UploadResult {
    data class Success(val uri: Uri, val remoteUrl: String) : UploadResult()
    data class Failure(val uri: Uri, val throwable: Throwable) : UploadResult()
}


