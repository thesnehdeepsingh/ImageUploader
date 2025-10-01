package com.esfersoft.imageuploader.data.media

import android.content.ContentResolver
import android.content.Context
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.esfersoft.imageuploader.domain.ImageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.io.BufferedInputStream

interface MediaStoreImageDataSource {
    suspend fun getDeviceImages(): List<ImageEntity>
}

class MediaStoreImageDataSourceImpl(
    private val context: Context
) : MediaStoreImageDataSource {
    override suspend fun getDeviceImages(): List<ImageEntity> = withContext(Dispatchers.IO) {
        val resolver: ContentResolver = context.contentResolver
        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.SIZE
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val images = mutableListOf<ImageEntity>()

        try {
            resolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val displayName = cursor.getString(nameCol)
                    val dateAdded = cursor.getLong(dateAddedCol)
                    val dateTaken = cursor.getLong(dateTakenCol)
                    val size = cursor.getLong(sizeCol)

                    val contentUri = ContentUris.withAppendedId(collection, id)
                    images.add(
                        ImageEntity(
                            id = id,
                            uri = contentUri,
                            displayName = displayName,
                            dateAddedEpochSeconds = if (dateTaken > 0) dateTaken / 1000 else dateAdded,
                            sizeBytes = size
                        )
                    )
                }
            }
        } catch (t: Throwable) {
            Log.w("MediaQuery", "query failed", t)
        }

        images
    }
}
