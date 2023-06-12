package com.ks.storage.file.space.hierarchy

import com.ks.storage.file.api.exceptions.StorageException
import com.ks.storage.file.space.blocks.BlockID
import java.io.*
import java.nio.charset.Charset

class NodeAlreadyExist(name: String) : StorageException("Node: $name already exist")

class NodeNotFound(name: String) : StorageException("node: $name doesn't exist")

sealed class Node : Serializable

internal data class FileNode(
    val startBlock: BlockID
) : Node()

typealias NodeDescription = Pair<String, Node>

internal class PathKey(val source: ByteArray) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PathKey

        return source.contentEquals(other.source)
    }

    override fun hashCode(): Int {
        return source.contentHashCode()
    }
}

internal class FolderNode : Node() {
    private val children: MutableMap<PathKey, Node>

    init {
        children = HashMap()
    }

    fun <T : Node> getNode(name: String): T? =
        children[PathKey(name.toByteArray(charset))] as? T

    fun <T : Node> addNode(name: String, node: T, merge: Boolean = false): T {
        val key = PathKey(name.toByteArray(charset))
        if (children.containsKey(key)) {
            val sourceNode = children[key]
            if (merge && node is FolderNode && sourceNode is FolderNode) {
                sourceNode.children.putAll(node.children) //here can be overriding ¯\_(ツ)_/¯
            } else {
                throw NodeAlreadyExist(name)
            }
        }
        children[key] = node
        return node
    }

    fun delete(name: String): Node {
        val key = PathKey(name.toByteArray(charset))
        if (!children.containsKey(key)) {
            throw NodeNotFound(name)
        }
        return children.remove(key) ?: throw ConcurrentModificationException()
    }

    fun list(): List<NodeDescription> = children.map { String(it.key.source, charset) to it.value }

    fun serialize(): ByteArray {
        val byteOut = ByteArrayOutputStream()
        val objOut = ObjectOutputStream(byteOut)
        objOut.writeObject(this)
        objOut.close()
        return byteOut.toByteArray()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FolderNode

        return children == other.children
    }

    override fun hashCode(): Int {
        return children.hashCode()
    }

    companion object {
        //current limitation, is not configurable
        val charset: Charset = Charset.forName("US-ASCII")

        fun deserialize(data: ByteArray): FolderNode {
            val objIn = ObjectInputStream(ByteArrayInputStream(data))
            val node: FolderNode = objIn.readObject() as FolderNode
            objIn.close()
            return node
        }
    }
}
