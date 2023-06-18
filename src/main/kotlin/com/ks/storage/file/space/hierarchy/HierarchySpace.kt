package com.ks.storage.file.space.hierarchy

import com.ks.storage.file.*
import com.ks.storage.file.api.exceptions.NoEmptySpace
import com.ks.storage.file.api.exceptions.Space
import com.ks.storage.file.api.exceptions.StorageException
import com.ks.storage.file.fromStart
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

fun rootFolders(separator: String) = listOf("", separator)

internal class NodeIsNotFolder(name: String) : StorageException("Node: $name is not a folder")

internal class HierarchySpace private constructor(
    private val spaceSize: Int, //bytes
    private val offset: Long,
    private val separator: String,
    private val channel: FileChannel
) : Closeable {

    init {
        assert(spaceSize >= 1024 * 1024) {
            "Current limitation minimum is 1MB, reason is my laziness ¯\\_(ツ)_/¯"
        }
    }

    private val root: FolderNode

    init {
        val buffer = ByteBuffer.allocate(spaceSize)
        channel.position(offset).read(buffer)
        val data = buffer.array()
        root = FolderNode.deserialize(data)
    }

    fun <T : Node> getNode(path: Location): T? {
        val nodeName = path.name(separator)
        return findParentFolder(path)?.getNode(nodeName)
    }

    fun deleteNode(location: Location): Node? {
        val parent = findParentFolder(location)
        return parent?.delete(location.name(separator))
    }

    fun <T : Node> createNode(path: Location, node: T, merge: Boolean = false): T {
        return getOrCreateFolder(path.parentName(separator))
            .addNode(path.name(separator), node, merge)
    }

    fun getOrCreateFolder(location: Location): FolderNode {
        var currentNode = root
        val splitLocation = location.split(separator)
        var nameIndex = 0
        while (nameIndex < splitLocation.size) {
            val nodeName = splitLocation[nameIndex]
            if (nodeName in rootFolders(separator)) {
                nameIndex++
                continue
            }
            val node = currentNode.getNode<Node>(nodeName) ?: break
            if (node is FileNode) {
                throw NodeIsNotFolder(nodeName)
            }
            currentNode = node as FolderNode
            nameIndex++
        }

        if (nameIndex == splitLocation.size) {
            return currentNode
        }

        while (nameIndex < splitLocation.size) {
            val nodeName = splitLocation[nameIndex++]
            currentNode = currentNode.addNode(nodeName, FolderNode())
        }

        return currentNode
    }

    private fun findParentFolder(location: Location): FolderNode? {
        var currentNode = root
        val splitPath = location.parentName(separator).split(separator)
        for (nodeName in splitPath) {
            if (nodeName in rootFolders(separator)) {
                currentNode = root
                continue
            }
            val node = currentNode.getNode<Node>(nodeName) ?: return null
            if (node is FileNode) {
                throw NodeIsNotFolder(nodeName)
            }
            currentNode = node as FolderNode
        }
        return currentNode
    }

    fun flush() {
        val data = root.serialize()
        if (data.size > spaceSize) {
            throw NoEmptySpace(Space.HIERARCHY)
        }
        channel.position(offset).write(ByteBuffer.wrap(data))
    }

    override fun close() {
        flush()
    }

    companion object {
        fun initNew(
            spaceSize: Int, //bytes
            offset: Long,
            separator: String,
            channel: FileChannel
        ): HierarchySpace {
            val buffer = ByteBuffer.allocate(spaceSize)
            val root = FolderNode()
            buffer.put(root.serialize())
            channel.position(offset).write(buffer.fromStart())

            return HierarchySpace(
                spaceSize = spaceSize,
                offset = offset,
                separator = separator,
                channel = channel
            )
        }

        fun load(
            spaceSize: Int, //bytes
            offset: Long,
            separator: String,
            channel: FileChannel
        ): HierarchySpace {
            return HierarchySpace(
                spaceSize = spaceSize,
                offset = offset,
                separator = separator,
                channel = channel
            )
        }
    }
}
