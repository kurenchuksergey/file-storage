package com.ks.storage.file.api

import com.ks.storage.file.Location
import com.ks.storage.file.metrics.StorageStat

class MeasuredStorage(
        id: String,
        private val storage: Storage
) : Storage by storage {

    private val metrics = StorageStat(id, storage.getSize())

    init {
        metrics.freeSpace = storage.getFreeSpace()
    }

    override fun getFile(location: Location): File {
        return storage.getFile(location).also { metrics.readsOperations++ }
    }

    override fun createFile(file: File) {
        storage.createFile(file)
                .also {
                    metrics.writesOperation++
                    metrics.freeSpace = storage.getFreeSpace()
                }
    }

    override fun delete(location: Location) {
        storage.delete(location)
                .also {
                    metrics.writesOperation++
                    metrics.freeSpace = storage.getFreeSpace()
                }
    }

    override fun close() {
        metrics.close()
        storage.close()
    }
}