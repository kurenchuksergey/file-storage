package com.ks.storage.file

import com.ks.storage.file.exceptions.NoEmptySpace
import com.ks.storage.file.exceptions.Space
import com.ks.storage.file.exceptions.StorageException
import java.nio.ByteBuffer

class ContentEmptyException : StorageException("Content is empty.")

internal typealias Content = ByteArray

internal class ContentOps(
    private val blockSpace: BlockSpace
) {

    private fun write(start: BlockID, content: Content): BlockID {
        if (content.isEmpty()) {
            throw ContentEmptyException()
        }

        var contentOffset = 0

        var previousBlock: Block? = null
        var currentBlock: Block
        var currentBlockID = start

        while (contentOffset < content.size) {
            currentBlock = blockSpace.createEmptyBlock(currentBlockID)
            currentBlock.fill(content, contentOffset)
            contentOffset += currentBlock.contentSize()
            if (previousBlock != null) {
                previousBlock.nextBlock = currentBlockID
                blockSpace.writeBlock(previousBlock)
            }
            previousBlock = currentBlock
            if (contentOffset < content.size) {
                currentBlockID = blockSpace.findNextEmptyBlock(currentBlockID + 1) ?: throw NoEmptySpace(Space.BLOCKS)
            }
        }
        if (previousBlock != null) {
            blockSpace.writeBlock(previousBlock)
        }

        return start
    }

    fun write(content: Content): BlockID {
        val firstEmptySpace = blockSpace.findNextEmptyBlock(-1) ?: throw NoEmptySpace(Space.BLOCKS)
        return write(firstEmptySpace, content)
    }

    fun read(start: BlockID): Content {
        val fileBlocks = ArrayList<Block>()
        var fileSize = 0

        var currentBlock = blockSpace.readBlock(start)
        while (currentBlock.nextBlock != EB) {
            fileBlocks.add(currentBlock)
            fileSize += currentBlock.contentSize()
            currentBlock = blockSpace.readBlock(currentBlock.nextBlock)
        }
        fileBlocks.add(currentBlock)
        fileSize += currentBlock.contentSize()

        val content = ByteBuffer.allocate(fileSize)
        for (block in fileBlocks) {
            content.put(block.getContent())
        }
        return content.array()
    }

    fun delete(start: BlockID) {
        var currentBlock = blockSpace.readBlock(start)
        while (currentBlock.nextBlock != EB) {
            blockSpace.delete(currentBlock.blockID)
            currentBlock = blockSpace.readBlock(currentBlock.nextBlock)
        }
    }

    fun append(start: BlockID, content: Content) {
        //find last block
        var currentBlock = blockSpace.readBlock(start)
        while (currentBlock.nextBlock != EB) {
            currentBlock = blockSpace.readBlock(currentBlock.nextBlock)
        }

        write(currentBlock.blockID, currentBlock.getContent() + content)
    }
}