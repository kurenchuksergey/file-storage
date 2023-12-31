package com.ks.storage.file

import java.nio.ByteBuffer

internal fun ByteBuffer.fromStart() = this.position(0)

typealias Location = String
fun Location.name(separator: String): String {
    val lastPosition = this.lastIndexOf(separator)
    if (lastPosition >= 0) {
        return this.substring(this.lastIndexOf(separator) + 1)
    }
    return this
}


fun Location.parentName(separator: String): String {
    val fileName = this.name(separator)
    if (fileName.length == this.length) {
        return ""
    }
    return this.substring(0, this.length - (this.name(separator).length + 1))
}

