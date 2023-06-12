package com.ks.storage.file.api

import com.ks.storage.file.Path
import com.ks.storage.file.space.hierarchy.FileNode
import com.ks.storage.file.space.hierarchy.FolderNode
import com.ks.storage.file.space.hierarchy.Node

fun Storage.getFile(path: Path): File {
    val fileNode = this.hierarchyOps.getFile(path)
    assert(fileNode.startBlock >= 0) {
        "start block should be gte 0, source: ${fileNode.startBlock}"
    }
    val content = this.contentOps.read(fileNode.startBlock)
    return File(path = path, content = content)
}

fun Storage.createFile(file: File) {
    val startBlockID = this.contentOps.write(content = file.content)
    try {
        this.hierarchyOps.createFile(file.path, startBlockID)
    } catch (e: Exception) {
        //todo logger
        this.contentOps.delete(startBlockID)
        throw e
    }
}

fun Storage.move(source: Path, dest: Path) {
    this.hierarchyOps.move(source, dest)
}

fun Storage.delete(path: Path) {
    val node = this.hierarchyOps.delete(path)
    delete(node)
}

private fun Storage.delete(node: Node) {
    if (node is FileNode) {
        this.contentOps.delete(node.startBlock)
    } else {
        (node as FolderNode).list().map { it.second }.forEach { delete(it) }
    }
}