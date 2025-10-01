package com.esfersoft.imageuploader.domain

import android.net.Uri
import com.esfersoft.imageuploader.data.media.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class LoadImagesUseCase(private val repository: MediaRepository) {
    operator fun invoke(): Flow<List<ImageEntity>> = repository.loadImages()
}

class ToggleSelectionModeUseCase {
    private val _mode = MutableStateFlow(SelectionMode.SINGLE)
    val mode = _mode.asStateFlow()
    fun toggle() {
        _mode.value = if (_mode.value == SelectionMode.SINGLE) SelectionMode.MULTI else SelectionMode.SINGLE
    }
    fun set(mode: SelectionMode) { _mode.value = mode }
}

class SelectImageUseCase(initial: Set<Uri> = emptySet()) {
    private val _selected = MutableStateFlow(initial)
    val selected = _selected.asStateFlow()

    fun toggle(uri: Uri, mode: SelectionMode) {
        val current = _selected.value.toMutableSet()
        if (mode == SelectionMode.SINGLE) {
            _selected.value = if (current.contains(uri)) emptySet() else setOf(uri)
            return
        }
        if (current.contains(uri)) current.remove(uri) else current.add(uri)
        _selected.value = current
    }

    fun selectAll(uris: Collection<Uri>) {
        _selected.value = uris.toSet()
    }

    fun clear() {
        _selected.value = emptySet()
    }
}

class AddMetadataUseCase {
    private val _metadata = MutableStateFlow<Map<Uri, ImageMetadataEntity>>(emptyMap())
    val metadata = _metadata.asStateFlow()

    fun set(uri: Uri, meta: ImageMetadataEntity) {
        _metadata.value = _metadata.value.toMutableMap().apply { put(uri, meta) }
    }
    fun get(uri: Uri): ImageMetadataEntity? = _metadata.value[uri]
}


