package com.ks.storage.file.api

import com.ks.storage.file.Location
import java.io.Closeable

interface Storage: Closeable {

    fun getSize(): Long

    fun getFreeSpace(): Long

    fun getFile(location: Location) : File

    fun createFile(file: File)

    fun move(source: Location, dest: Location)

    fun delete(location: Location)
}