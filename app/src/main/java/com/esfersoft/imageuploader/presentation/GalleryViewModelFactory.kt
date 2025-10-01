package com.esfersoft.imageuploader.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.esfersoft.imageuploader.di.ServiceLocator
import com.esfersoft.imageuploader.domain.AddMetadataUseCase
import com.esfersoft.imageuploader.domain.LoadImagesUseCase
import com.esfersoft.imageuploader.domain.SelectImageUseCase
import com.esfersoft.imageuploader.domain.ToggleSelectionModeUseCase

class GalleryViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
            val repo = ServiceLocator.mediaRepository
            val storageRepo = ServiceLocator.storageRepository
            val pending = ServiceLocator.pendingUploadsStore
            return GalleryViewModel(
                loadImagesUseCase = LoadImagesUseCase(repo),
                storageRepository = storageRepo,
                toggleSelectionModeUseCase = ToggleSelectionModeUseCase(),
                selectImageUseCase = SelectImageUseCase(),
                addMetadataUseCase = AddMetadataUseCase(),
                pendingUploadsStore = pending
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${'$'}modelClass")
    }
}


