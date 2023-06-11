package com.ks.storage.file.space.hierarchy

import com.ks.storage.file.exceptions.NoEmptySpace
import com.ks.storage.file.exceptions.Space
import com.ks.storage.file.exceptions.StorageException
import com.ks.storage.file.fromStart
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

typealias Path = String

fun Path.name(separator: String): String {
    val lastPosition = this.lastIndexOf(separator)
    if (lastPosition >= 0) {
        return this.substring(this.lastIndexOf(separator) + 1)
    }
    return this
}

fun rootFolders(separator: String) = listOf("", separator)

fun Path.parentName(separator: String): String {
    val fileName = this.name(separator)
    if (fileName.length == this.length) {
        return ""
    }
    return this.substring(0, this.length - (this.name(separator).length + 1))
}

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

    fun <T : Node> getNode(path: Path): T? {
        val nodeName = path.name(separator)
        return findParentFolder(path)?.getNode(nodeName)
    }

    fun deleteNode(path: Path): Node? {
        val parent = findParentFolder(path)
        return parent?.delete(path.name(separator))
    }

    fun <T : Node> createNode(path: Path, node: T, merge: Boolean = false): T {
        return getOrCreateFolder(path.parentName(separator))
            .addNode(path.name(separator), node, merge)
    }

    fun getOrCreateFolder(path: Path): FolderNode {
        var currentNode = root
        val splitPath = path.split(separator)
        var nameIndex = 0
        while (nameIndex < splitPath.size) {
            val nodeName = splitPath[nameIndex]
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

        if (nameIndex == splitPath.size) {
            return currentNode
        }

        while (nameIndex < splitPath.size) {
            val nodeName = splitPath[nameIndex++]
            currentNode = currentNode.addNode(nodeName, FolderNode())
        }

        return currentNode
    }

    private fun findParentFolder(path: Path): FolderNode? {
        var currentNode = root
        val splitPath = path.parentName(separator).split(separator)
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
