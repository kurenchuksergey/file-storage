package com.ks.storage.file.api

import com.ks.storage.file.Location
import java.io.FileOutputStream
import java.nio.file.Path


data class File(val location: Location, val content: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as File

        if (location != other.location) return false
        return content.contentEquals(other.content)
    }

    override fun hashCode(): Int {
        var result = location.hashCode()
        result = 31 * result + content.contentHashCode()
        return result
    }

    fun export(path: Path) {
        FileOutputStream(path.toFile()).use {
            it.write(content)
        }
    }
}