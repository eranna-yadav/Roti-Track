package com.sipwell.app.store

import android.content.Context
import java.io.File
import java.util.concurrent.Executors

/** Persists the state as one JSON file, written off the main thread and swapped in atomically. */
class AndroidStorage(context: Context, name: String) : Storage {
    private val file = File(context.filesDir, name)
    private val writer = Executors.newSingleThreadExecutor()

    override fun load(): String? = runCatching { file.takeIf { it.exists() }?.readText() }.getOrNull()

    override fun save(text: String) {
        writer.execute {
            runCatching {
                val tmp = File(file.parentFile, "${file.name}.tmp")
                tmp.writeText(text)
                tmp.renameTo(file)
            }
        }
    }

    override fun clear() {
        writer.execute { file.delete() }
    }
}
