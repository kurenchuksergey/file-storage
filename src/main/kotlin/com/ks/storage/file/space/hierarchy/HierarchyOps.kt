package com.ks.storage.file.space.hierarchy

import com.ks.storage.file.Location
import com.ks.storage.file.api.exceptions.StorageException
import com.ks.storage.file.space.blocks.BlockID
import java.util.logging.Level
import java.util.logging.Logger

class NodeIsFolder(name: String) : StorageException("Node: $name is a folder")

internal class HierarchyOps(
        private val hierarchySpace: HierarchySpace
) {
    fun createFile(location: Location, startBlockID: BlockID): FileNode {
        return hierarchySpace.createNode(location, FileNode(startBlockID)).also {
            hierarchySpace.flush()
            logger.log(Level.INFO, "new file: $location[$startBlockID]")
        }
    }

    fun createFolder(location: Location): FolderNode {
        return hierarchySpace.createNode(location, FolderNode()).also {
            hierarchySpace.flush()
            logger.log(Level.INFO, "new folder: $location")
        }
    }

    fun getFile(location: Location): FileNode {
        logger.log(Level.INFO, "access to $location")
        val node: Node = hierarchySpace.getNode(location) ?: throw NodeNotFound(location)
        if (node is FolderNode) {
            throw NodeIsFolder(location)
        }
        return node as FileNode
    }

    fun getFolder(location: Location): FolderNode {
        logger.log(Level.INFO, "access to $location")
        val node: Node = hierarchySpace.getNode(location) ?: throw NodeNotFound(location)
        if (node is FileNode) {
            throw NodeIsNotFolder(location)
        }
        return node as FolderNode
    }

    fun delete(location: Location): Node {
        logger.log(Level.INFO, "removing $location")
        return (hierarchySpace.deleteNode(location) ?: throw NodeNotFound(location)).also {
            hierarchySpace.flush()
            logger.log(Level.INFO, "was removed $location")
        }
    }

    fun move(sourceLocation: Location, dest: Location): Node {
        logger.log(Level.INFO, "moving from $sourceLocation to $dest")
        val newPointer = hierarchySpace.createNode(
                path = dest,
                node = hierarchySpace.getNode<Node>(sourceLocation) ?: throw NodeNotFound(sourceLocation),
                merge = true
        )
        hierarchySpace.deleteNode(sourceLocation)
        hierarchySpace.flush()
        logger.log(Level.INFO, "moved from $sourceLocation to $dest")
        return newPointer
    }

    companion object {
        private val logger: Logger = Logger.getLogger(HierarchyOps::class.java.toString())
    }
}