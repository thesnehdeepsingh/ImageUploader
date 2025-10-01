package com.esfersoft.imageuploader.data.storage

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import com.esfersoft.imageuploader.domain.ImageMetadataEntity
import com.esfersoft.imageuploader.domain.UploadProgressEntity
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.BufferedInputStream

class RealtimeBase64Repository(
    private val db: FirebaseDatabase,
    private val contentResolver: ContentResolver
) : StorageRepository {

    override fun uploadImage(uri: Uri, metadata: ImageMetadataEntity): Flow<UploadProgressEntity> = flow {
        val key = db.reference.child("uploads").push().key ?: System.currentTimeMillis().toString()
        val ref = db.reference.child("uploads").child(key)
        val pfd = contentResolver.openFileDescriptor(uri, "r")
        val totalBytes = pfd?.statSize ?: -1L
        val input = BufferedInputStream(contentResolver.openInputStream(uri))

        val chunkSize = 128 * 1024
        val buffer = ByteArray(chunkSize)
        var read: Int
        var sent = 0L
        var index = 0

        val metaMap = mapOf(
            "uri" to uri.toString(),
            "caption" to metadata.caption,
            "tags" to metadata.tags
        )
        ref.child("meta").setValue(metaMap)

        while (input.read(buffer).also { read = it } != -1) {
            val base64 = Base64.encodeToString(buffer.copyOf(read), Base64.NO_WRAP)
            ref.child("chunks").child(index.toString()).setValue(base64)
            sent += read
            index++
            emit(UploadProgressEntity(uri, progress = if (totalBytes > 0) sent.toFloat() / totalBytes else 0f, bytesSent = sent, totalBytes = totalBytes))
        }

        input.close()
        pfd?.close()

        ref.child("status").setValue("complete")
        emit(UploadProgressEntity(uri, progress = 1f, bytesSent = totalBytes, totalBytes = totalBytes, isCompleted = true))
    }
}
