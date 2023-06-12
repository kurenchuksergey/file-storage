package com.ks.storage.file.api

import com.ks.storage.file.space.blocks.BlockSpace
import com.ks.storage.file.space.blocks.ContentOps
import com.ks.storage.file.space.hierarchy.HierarchyOps
import com.ks.storage.file.space.hierarchy.HierarchySpace
import com.ks.storage.file.space.meta.Meta
import java.io.Closeable
import java.io.RandomAccessFile
import java.nio.file.Path

class Storage internal constructor(
    path: Path
) : Closeable {

    private val meta: Meta
    private var hierarchySpace: HierarchySpace
    private val blockSpace: BlockSpace
    private val accessFile = RandomAccessFile(path.toFile(), "rw")

    internal val hierarchyOps: HierarchyOps
    internal val contentOps: ContentOps

    init {
        meta = Meta.loadFromFile(accessFile.channel, 0)
        hierarchySpace = HierarchySpace.load(
            spaceSize = meta.hierarchySpaceSize,
            offset = Meta.OBJECT_SIZE.toLong(),
            separator = FILE_SEPARATOR,
            channel = accessFile.channel
        )
        blockSpace = BlockSpace(
            blockSize = meta.blockSize,
            blockCount = meta.blockCounter,
            offset = Meta.OBJECT_SIZE.toLong() + meta.hierarchySpaceSize,
            channel = accessFile.channel
        )

        hierarchyOps = HierarchyOps(hierarchySpace)
        contentOps = ContentOps(blockSpace)
    }

    override fun close() {
        hierarchySpace.close()
        blockSpace.close()
        accessFile.close()
    }

    companion object {
        const val FILE_SEPARATOR = "/"

        fun new(): StorageBuilder = StorageBuilder()
    }
}


