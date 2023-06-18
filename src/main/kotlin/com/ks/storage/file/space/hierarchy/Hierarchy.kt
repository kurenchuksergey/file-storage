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

internal class FolderNode : Node() {
    private val children: MutableMap<String, Node>

    init {
        children = HashMap()
    }

    fun <T : Node> getNode(name: String): T? =
        children[name] as? T

    fun <T : Node> addNode(name: String, node: T, merge: Boolean = false): T {
        if (children.containsKey(name)) {
            val sourceNode = children[name]
            if (merge && node is FolderNode && sourceNode is FolderNode) {
                sourceNode.children.putAll(node.children) //here can be overriding ¯\_(ツ)_/¯
            } else {
                throw NodeAlreadyExist(name)
            }
        }
        children[name] = node
        return node
    }

    fun delete(name: String): Node {
        if (!children.containsKey(name)) {
            throw NodeNotFound(name)
        }
        return children.remove(name) ?: throw ConcurrentModificationException()
    }

    fun list(): List<NodeDescription> = children.map { it.key to it.value }

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
