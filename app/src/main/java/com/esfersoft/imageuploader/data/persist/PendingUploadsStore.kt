package com.esfersoft.imageuploader.data.persist

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.esfersoft.imageuploader.domain.ImageMetadataEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "pending_uploads")

class PendingUploadsStore(private val context: Context) {
    private val KEY_JSON = stringPreferencesKey("pending")

    fun observe(): Flow<Map<Uri, ImageMetadataEntity>> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_JSON]?.let { decode(it) } ?: emptyMap()
        }

    suspend fun save(map: Map<Uri, ImageMetadataEntity>) {
        context.dataStore.edit { prefs -> prefs[KEY_JSON] = encode(map) }
    }

    suspend fun loadOnce(): Map<Uri, ImageMetadataEntity> = observe().first()

    private fun encode(map: Map<Uri, ImageMetadataEntity>): String {
        val arr = JSONArray()
        map.forEach { (uri, meta) ->
            val obj = JSONObject()
            obj.put("uri", uri.toString())
            obj.put("caption", meta.caption)
            obj.put("tags", JSONArray(meta.tags))
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun decode(json: String): Map<Uri, ImageMetadataEntity> {
        val arr = JSONArray(json)
        val map = LinkedHashMap<Uri, ImageMetadataEntity>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val uri = Uri.parse(obj.getString("uri"))
            val caption = obj.optString("caption", "")
            val tagsJson = obj.optJSONArray("tags") ?: JSONArray()
            val tags = buildList(tagsJson.length()) { for (j in 0 until tagsJson.length()) add(tagsJson.getString(j)) }
            map[uri] = ImageMetadataEntity(caption, tags)
        }
        return map
    }
}
