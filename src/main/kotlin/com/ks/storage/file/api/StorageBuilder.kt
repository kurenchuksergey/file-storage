package com.ks.storage.file.api

import com.ks.storage.file.space.blocks.Block
import com.ks.storage.file.space.hierarchy.HierarchySpace
import com.ks.storage.file.space.meta.Meta
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.file.Path

class StorageBuilder {

    private enum class StorageType {
        FAST, EFFICIENT
    }

    private enum class StorageHierarchyType {
        SMALL, HUGE
    }

    private var hierarchyType: StorageHierarchyType = StorageHierarchyType.SMALL
    private var storageType: StorageType = StorageType.EFFICIENT
    private var expectedSize: Int = 100 * 1024 * 1024 //100MB
    private var storagePath: Path? = null
    private var measured = false
    private var id: String? = null

    fun fast(): StorageBuilder {
        storageType = StorageType.FAST
        return this
    }

    fun id(id: String): StorageBuilder {
        this.id = id
        return this
    }

    fun measured(measured: Boolean): StorageBuilder {
        this.measured = measured
        return this
    }

    fun efficient(): StorageBuilder {
        storageType = StorageType.EFFICIENT
        return this
    }

    fun size(bytes: Int): StorageBuilder {
        expectedSize = bytes
        return this
    }

    fun smallHierarchy(): StorageBuilder {
        hierarchyType = StorageHierarchyType.SMALL
        return this
    }

    fun hugeHierarchy(): StorageBuilder {
        hierarchyType = StorageHierarchyType.HUGE
        return this
    }

    fun path(path: Path): StorageBuilder {
        this.storagePath = path
        return this
    }

    fun build(): Storage {
        assert(expectedSize > 1024) {
            "minimum 1KB"
        }
        assert(storagePath != null) {
            "path should be setup"
        }

        //predefined params
        val blockSize = if (storageType == StorageType.EFFICIENT) 32 * 1024 else 128 * 1024
        val hierarchySpaceSize =
                if (hierarchyType == StorageHierarchyType.SMALL) 1 * 1024 * 1024 else 2 * 1024 * 1024
        var blockCounter = expectedSize / Block.efficiently(blockSize) + 1
        blockCounter += 8 - blockCounter % 8

        val meta = Meta(
                blockCounter = blockCounter,
                blockSize = blockSize,
                hierarchySpaceSize = hierarchySpaceSize
        )

        assert(id != null){
            "ID should be present"
        }

        RandomAccessFile(storagePath!!.toFile(), "rw").use {
            it.channel.write(ByteBuffer.wrap(meta.serialize()))
            HierarchySpace.initNew(
                    spaceSize = meta.hierarchySpaceSize,
                    offset = Meta.OBJECT_SIZE.toLong(),
                    separator = StorageImpl.FILE_SEPARATOR,
                    it.channel
            )
        }

        return StorageImpl(storagePath!!).let {
            if (measured) {
                MeasuredStorage(id!!, it)
            } else {
                it
            }
        }
    }
}