package com.ks.storage.file.api

import com.ks.storage.file.Location
import com.ks.storage.file.space.blocks.BlockSpace
import com.ks.storage.file.space.blocks.ContentOps
import com.ks.storage.file.space.hierarchy.*
import com.ks.storage.file.space.meta.Meta
import java.io.Closeable
import java.io.RandomAccessFile
import java.nio.file.Path
import java.util.logging.Level
import java.util.logging.Logger

class StorageImpl internal constructor(
        path: Path
) : Closeable, Storage {

    private var hierarchySpace: HierarchySpace
    private val blockSpace: BlockSpace
    private val accessFile = RandomAccessFile(path.toFile(), "rw")
    private val size: Long

    private val hierarchyOps: HierarchyOps
    private val contentOps: ContentOps

    init {
        val meta = Meta.loadFromFile(accessFile.channel, 0)
        size = meta.blockSize.toLong() * meta.blockSize
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

    override fun getSize() = size
    override fun getFreeSpace() = contentOps.getFreeSpace()

    override fun getFile(location: Location): File {
        val fileNode = this.hierarchyOps.getFile(location)
        assert(fileNode.startBlock >= 0) {
            "start block should be gte 0, source: ${fileNode.startBlock}"
        }
        val content = this.contentOps.read(fileNode.startBlock)
        return File(location = location, content = content)
    }

    override fun createFile(file: File) {
        val startBlockID = this.contentOps.write(content = file.content)
        try {
            this.hierarchyOps.createFile(file.location, startBlockID)
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Can't create new file: ${file.location}. Going to remove created blocks", e)
            this.contentOps.delete(startBlockID)
            throw e
        }
    }

    override fun move(source: Location, dest: Location) {
        this.hierarchyOps.move(source, dest)
    }

    override fun delete(location: Location) {
        val node = this.hierarchyOps.delete(location)
        delete(node)
    }

    private fun StorageImpl.delete(node: Node) {
        if (node is FileNode) {
            this.contentOps.delete(node.startBlock)
        } else {
            (node as FolderNode).list().map { it.second }.forEach { delete(it) }
        }
    }

    override fun close() {
        hierarchySpace.close()
        blockSpace.close()
        accessFile.close()
    }

    companion object {
        const val FILE_SEPARATOR = "/"

        private val logger: Logger = Logger.getLogger(StorageImpl::class.java.simpleName)

        fun new(): StorageBuilder = StorageBuilder()
    }
}


