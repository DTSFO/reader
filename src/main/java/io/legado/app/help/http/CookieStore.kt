@file:Suppress("unused")

package io.legado.app.help.http

import io.legado.app.utils.TextUtils
import io.legado.app.help.CacheManager
import io.legado.app.help.http.api.CookieManager
import java.util.concurrent.ConcurrentHashMap

object CookieStore : CookieManager {

    private const val CACHE_PREFIX = "cookie_"
    private val cookieMap = ConcurrentHashMap<String, String>()

    private fun normalizeKey(url: String?): String {
        return url?.trim().orEmpty()
    }

    private fun cacheKey(url: String): String {
        return "$CACHE_PREFIX$url"
    }

    override fun setCookie(url: String, cookie: String?) {
        val key = normalizeKey(url)
        if (key.isEmpty()) return
        if (cookie.isNullOrBlank()) {
            removeCookie(key)
            return
        }
        cookieMap[key] = cookie
        CacheManager.put(cacheKey(key), cookie)
    }

    override fun replaceCookie(url: String, cookie: String) {
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(cookie)) {
            return
        }
        val oldCookie = getCookie(url)
        if (TextUtils.isEmpty(oldCookie)) {
            setCookie(url, cookie)
        } else {
            val cookieMap = cookieToMap(oldCookie)
            cookieMap.putAll(cookieToMap(cookie))
            val newCookie = mapToCookie(cookieMap)
            setCookie(url, newCookie)
        }
    }

    override fun getCookie(url: String): String {
        val key = normalizeKey(url)
        if (key.isEmpty()) return ""
        cookieMap[key]?.let { return it }
        return CacheManager.get(cacheKey(key))?.also {
            cookieMap[key] = it
        } ?: ""
    }

    override fun removeCookie(url: String) {
        val key = normalizeKey(url)
        if (key.isEmpty()) return
        cookieMap.remove(key)
        CacheManager.delete(cacheKey(key))
    }

    override fun cookieToMap(cookie: String): MutableMap<String, String> {
        val cookieMap = mutableMapOf<String, String>()
        if (cookie.isBlank()) {
            return cookieMap
        }
        val pairArray = cookie.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (pair in pairArray) {
            val splitIndex = pair.indexOf("=")
            if (splitIndex <= 0) {
                continue
            }
            val key = pair.substring(0, splitIndex).trim { it <= ' ' }
            val value = pair.substring(splitIndex + 1)
            if (value.isNotBlank() || value.trim { it <= ' ' } == "null") {
                cookieMap[key] = value.trim { it <= ' ' }
            }
        }
        return cookieMap
    }

    override fun mapToCookie(cookieMap: Map<String, String>?): String? {
        if (cookieMap == null || cookieMap.isEmpty()) {
            return null
        }
        val builder = StringBuilder()
        for (key in cookieMap.keys) {
            val value = cookieMap[key]
            if (value?.isNotBlank() == true) {
                builder.append(key)
                    .append("=")
                    .append(value)
                    .append(";")
            }
        }
        return builder.deleteCharAt(builder.lastIndexOf(";")).toString()
    }

    fun clear() {
        cookieMap.keys.forEach { CacheManager.delete(cacheKey(it)) }
        cookieMap.clear()
    }

    fun saveFromResponse(url: String, setCookieHeaders: List<String>) {
        if (setCookieHeaders.isEmpty()) return
        val oldCookieMap = cookieToMap(getCookie(url))
        for (header in setCookieHeaders) {
            val pair = header.substringBefore(";").trim()
            val splitIndex = pair.indexOf("=")
            if (splitIndex <= 0) continue
            val key = pair.substring(0, splitIndex).trim()
            val value = pair.substring(splitIndex + 1).trim()
            if (value.isEmpty()) {
                oldCookieMap.remove(key)
            } else {
                oldCookieMap[key] = value
            }
        }
        setCookie(url, mapToCookie(oldCookieMap))
    }

}
