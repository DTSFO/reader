package io.legado.app.help

import io.legado.app.model.analyzeRule.QueryTTF
import io.legado.app.utils.ACache
import java.util.concurrent.ConcurrentHashMap

@Suppress("unused")
object CacheManager {

    private val queryTTFMap = hashMapOf<String, Pair<Long, QueryTTF>>()
    private val stringCacheMap = ConcurrentHashMap<String, Pair<Long, String>>()

    /**
     * saveTime 单位为秒
     */
    @JvmOverloads
    fun put(key: String, value: Any, saveTime: Int = 0) {
        val deadline =
            if (saveTime == 0) 0 else System.currentTimeMillis() + saveTime * 1000
        when (value) {
            is QueryTTF -> queryTTFMap[key] = Pair(deadline, value)
            is ByteArray -> ACache.get().put(key, value, saveTime)
            else -> {
                val cacheValue = value.toString()
                stringCacheMap[key] = Pair(deadline, cacheValue)
                ACache.get().put(key, cacheValue, saveTime)
            }
        }
    }

    fun get(key: String): String? {
        stringCacheMap[key]?.let { cache ->
            if (cache.first == 0L || cache.first > System.currentTimeMillis()) {
                return cache.second
            }
            stringCacheMap.remove(key)
        }
        return ACache.get().getAsString(key)?.also {
            stringCacheMap[key] = Pair(0L, it)
        }
    }

    fun getInt(key: String): Int? {
        return get(key)?.toIntOrNull()
    }

    fun getLong(key: String): Long? {
        return get(key)?.toLongOrNull()
    }

    fun getDouble(key: String): Double? {
        return get(key)?.toDoubleOrNull()
    }

    fun getFloat(key: String): Float? {
        return get(key)?.toFloatOrNull()
    }

    fun getByteArray(key: String): ByteArray? {
        return ACache.get().getAsBinary(key)
    }

    fun getQueryTTF(key: String): QueryTTF? {
        val cache = queryTTFMap[key] ?: return null
        if (cache.first == 0L || cache.first > System.currentTimeMillis()) {
            return cache.second
        }
        return null
    }

    fun putFile(key: String, value: String, saveTime: Int = 0) {
        ACache.get().put(key, value, saveTime)
    }

    fun getFile(key: String): String? {
        return ACache.get().getAsString(key)
    }

    fun delete(key: String) {
        stringCacheMap.remove(key)
        ACache.get().remove(key)
    }
}
