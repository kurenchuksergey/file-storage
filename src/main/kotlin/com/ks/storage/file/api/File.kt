package com.ks.storage.file.api

import com.ks.storage.file.Content
import com.ks.storage.file.Path
import java.io.FileOutputStream
import java.nio.file.Path as JavaPath


data class File(val path: Path, val content: Content) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as File

        if (path != other.path) return false
        return content.contentEquals(other.content)
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + content.contentHashCode()
        return result
    }

    fun export(path: JavaPath) {
        val fileOutputStream = FileOutputStream(path.toFile()).use {
            it.write(content)
        }
    }
}