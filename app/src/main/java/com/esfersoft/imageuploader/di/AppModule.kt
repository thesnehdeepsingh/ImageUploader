package com.esfersoft.imageuploader.di

import android.content.Context
import com.esfersoft.imageuploader.data.media.*
import com.esfersoft.imageuploader.data.persist.PendingUploadsStore
import com.esfersoft.imageuploader.data.storage.RealtimeBase64Repository
import com.esfersoft.imageuploader.data.storage.StorageRepository
import com.google.firebase.database.FirebaseDatabase

object ServiceLocator {
    @Volatile private var initialized = false
    private lateinit var appContext: Context

    lateinit var mediaStoreDataSource: MediaStoreImageDataSource
        private set
    lateinit var mediaRepository: MediaRepository
        private set
    lateinit var realtimeDb: FirebaseDatabase
        private set
    lateinit var storageRepository: StorageRepository
        private set
    lateinit var pendingUploadsStore: PendingUploadsStore
        private set

    fun init(context: Context) {
        if (initialized) return
        appContext = context.applicationContext
        mediaStoreDataSource = MediaStoreImageDataSourceImpl(appContext)
        mediaRepository = MediaRepositoryImpl(mediaStoreDataSource)
        realtimeDb = FirebaseDatabase.getInstance()
        storageRepository = RealtimeBase64Repository(realtimeDb, appContext.contentResolver)
        pendingUploadsStore = PendingUploadsStore(appContext)
        initialized = true
    }
}
