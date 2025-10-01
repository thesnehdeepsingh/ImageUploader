package com.esfersoft.imageuploader.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esfersoft.imageuploader.data.storage.StorageRepository
import com.esfersoft.imageuploader.domain.*
import com.esfersoft.imageuploader.data.persist.PendingUploadsStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GalleryViewModel(
    private val loadImagesUseCase: LoadImagesUseCase,
    private val storageRepository: StorageRepository,
    private val toggleSelectionModeUseCase: ToggleSelectionModeUseCase,
    private val selectImageUseCase: SelectImageUseCase,
    private val addMetadataUseCase: AddMetadataUseCase,
    private val pendingUploadsStore: PendingUploadsStore,
) : ViewModel() {

    private val _images = MutableStateFlow<List<ImageEntity>>(emptyList())
    val images: StateFlow<List<ImageEntity>> = _images.asStateFlow()

    val selectionMode: StateFlow<SelectionMode> = toggleSelectionModeUseCase.mode
        .stateIn(viewModelScope, SharingStarted.Eagerly, SelectionMode.SINGLE)

    val selectedUris: StateFlow<Set<Uri>> = selectImageUseCase.selected
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val metadataMap: StateFlow<Map<Uri, ImageMetadataEntity>> = addMetadataUseCase.metadata
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _progress = MutableStateFlow<Map<Uri, UploadProgressEntity>>(emptyMap())
    val progress: StateFlow<Map<Uri, UploadProgressEntity>> = _progress.asStateFlow()

    private val _resumePrompt = MutableStateFlow<Map<Uri, ImageMetadataEntity>>(emptyMap())
    val resumePrompt: StateFlow<Map<Uri, ImageMetadataEntity>> = _resumePrompt.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun toggleSelectionMode() {
        toggleSelectionModeUseCase.toggle()
    }

    fun enterSelectionMode() {
        toggleSelectionModeUseCase.set(SelectionMode.MULTI)
    }

    fun toggleSelect(uri: Uri) {
        selectImageUseCase.toggle(uri, selectionMode.value)
    }

    fun setMetadata(uri: Uri, metadata: ImageMetadataEntity) {
        addMetadataUseCase.set(uri, metadata)
    }

    private val ongoingUploads: MutableMap<Uri, Job> = mutableMapOf()

    fun uploadSelectedWithRetry(maxRetries: Int = 2) {
        // Reset progress for all selected items so their bars appear and re-uploads start fresh
        val selectedNow = selectedUris.value
        if (selectedNow.isEmpty()) return
        _progress.update { current ->
            val mutable = current.toMutableMap()
            selectedNow.forEach { uri ->
                mutable[uri] = UploadProgressEntity(uri = uri, progress = 0f)
            }
            mutable
        }
        selectedNow.forEach { uri ->
            if (ongoingUploads.containsKey(uri)) return@forEach
            val meta = metadataMap.value[uri] ?: ImageMetadataEntity()
            val job = viewModelScope.launch {
                var attempt = 0
                while (attempt <= maxRetries) {
                    try {
                        storageRepository.uploadImage(uri, meta).collect { p ->
                            _progress.update { it.toMutableMap().apply { put(uri, p) } }
                        }
                        // mark completed and clean up
                        _progress.update { it.toMutableMap().apply {
                            val current = it[uri]
                            put(uri, UploadProgressEntity(uri = uri, progress = 1f, bytesSent = current?.bytesSent ?: 0L, totalBytes = current?.totalBytes ?: 0L, isCompleted = true))
                        } }
                        selectImageUseCase.toggle(uri, SelectionMode.MULTI) // deselect
                        persistPending()
                        _events.emit("Uploaded successfully")
                        break
                    } catch (t: Throwable) {
                        attempt++
                        if (attempt > maxRetries) {
                            _progress.update {
                                it.toMutableMap().apply {
                                    put(uri, UploadProgressEntity(uri, progress = _progress.value[uri]?.progress ?: 0f, errorMessage = t.message))
                                }
                            }
                            _events.emit("Upload failed: ${t.message ?: "unknown error"}")
                        }
                    }
                }
                ongoingUploads.remove(uri)
            }
            ongoingUploads[uri] = job
        }
    }

    init {
        // initial lazy load; UI may call refresh after permission grant
        viewModelScope.launch { refreshImages() }
        viewModelScope.launch {
            val saved = pendingUploadsStore.loadOnce()
            if (saved.isNotEmpty()) {
                _resumePrompt.value = saved
            }
        }
        viewModelScope.launch {
            combine(selectedUris, metadataMap) { selected, meta ->
                selected.associateWith { meta[it] ?: ImageMetadataEntity() }
            }.collect { map ->
                pendingUploadsStore.save(map)
            }
        }
    }

    private fun persistPending() {
        viewModelScope.launch {
            val map = selectedUris.value.associateWith { metadataMap.value[it] ?: ImageMetadataEntity() }
            pendingUploadsStore.save(map)
        }
    }

    fun resumeSavedUploads() {
        val saved = _resumePrompt.value
        if (saved.isNotEmpty()) {
            saved.forEach { (uri, meta) ->
                addMetadataUseCase.set(uri, meta)
            }
            viewModelScope.launch {
                // select all
                saved.keys.forEach { uri -> selectImageUseCase.toggle(uri, SelectionMode.MULTI) }
            }
            uploadSelectedWithRetry()
            _resumePrompt.value = emptyMap()
        }
    }

    fun selectAllVisible(uris: List<Uri>) {
        selectImageUseCase.selectAll(uris)
    }

    fun clearSelection() { selectImageUseCase.clear() }

    suspend fun refreshImages() {
        try {
            loadImagesUseCase().collect { list ->
                if (list != _images.value) {
                    _images.value = list
                }
            }
        } catch (_: Throwable) { }
    }

    fun setPickedUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val now = System.currentTimeMillis() / 1000
        val entities = uris.mapIndexed { idx, uri ->
            ImageEntity(
                id = now + idx,
                uri = uri,
                displayName = uri.lastPathSegment,
                dateAddedEpochSeconds = now,
                sizeBytes = 0
            )
        }
        _images.value = entities
    }
}


