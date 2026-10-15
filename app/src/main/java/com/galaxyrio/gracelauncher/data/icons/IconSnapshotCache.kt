package com.galaxyrio.gracelauncher.data.icons

/** Keep only the latest revision of each icon, rather than every theme ever used. */
internal class IconSnapshotCache<S, V>(private val capacity: Int = 256) {
    init { require(capacity > 0) }
    private data class Entry<S, V>(val signature: S, val value: V)
    private val entries = LinkedHashMap<String, Entry<S, V>>(16, 0.75f, true)

    @Synchronized
    fun get(key: String, signature: S): V? = entries[key]?.takeIf { it.signature == signature }?.value

    @Synchronized
    fun put(key: String, signature: S, value: V) {
        entries[key] = Entry(signature, value)
        while (entries.size > capacity) entries.remove(entries.keys.first())
    }
}
