package com.ks.storage.file.api

import com.ks.storage.file.Location

interface StorageOps {
    fun getFile(location: Location) : File

    fun createFile(file: File)

    fun move(source: Location, dest: Location)

    fun delete(location: Location)
}