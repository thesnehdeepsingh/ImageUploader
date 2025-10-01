package com.esfersoft.imageuploader.data.media

import com.esfersoft.imageuploader.domain.ImageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface MediaRepository {
    fun loadImages(): Flow<List<ImageEntity>>
}

class MediaRepositoryImpl(
    private val dataSource: MediaStoreImageDataSource
) : MediaRepository {
    override fun loadImages(): Flow<List<ImageEntity>> = flow {
        emit(dataSource.getDeviceImages())
    }
}
