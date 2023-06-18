package com.ks.storage.file.space.hierarchy

import com.ks.storage.file.Location
import com.ks.storage.file.api.exceptions.StorageException
import com.ks.storage.file.space.blocks.BlockID

class NodeIsFolder(name: String) : StorageException("Node: $name is a folder")

internal class HierarchyOps(
    private val hierarchySpace: HierarchySpace
) {
    fun createFile(path: Location, startBlockID: BlockID): FileNode {
        return hierarchySpace.createNode(path, FileNode(startBlockID)).also { hierarchySpace.flush() }
    }

    fun createFolder(path: Location): FolderNode {
        return hierarchySpace.createNode(path, FolderNode()).also { hierarchySpace.flush() }
    }

    fun getFile(path: Location): FileNode {
        val node: Node = hierarchySpace.getNode(path) ?: throw NodeNotFound(path)
        if (node is FolderNode) {
            throw NodeIsFolder(path)
        }
        return node as FileNode
    }

    fun getFolder(path: Location): FolderNode {
        val node: Node = hierarchySpace.getNode(path) ?: throw NodeNotFound(path)
        if (node is FileNode) {
            throw NodeIsNotFolder(path)
        }
        return node as FolderNode
    }

    fun delete(path: Location): Node {
        return (hierarchySpace.deleteNode(path) ?: throw NodeNotFound(path)).also { hierarchySpace.flush() }
    }

    fun move(sourcePath: Location, dest: Location): Node {
        val newPointer = hierarchySpace.createNode(
                path = dest,
                node = hierarchySpace.getNode<Node>(sourcePath) ?: throw NodeNotFound(sourcePath),
                merge = true
        )
        hierarchySpace.deleteNode(sourcePath)
        hierarchySpace.flush()
        return newPointer
    }
}