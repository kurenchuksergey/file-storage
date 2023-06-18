package com.ks.storage.file.space.blocks

import com.ks.storage.file.api.exceptions.NoEmptySpace
import com.ks.storage.file.api.exceptions.Space
import com.ks.storage.file.api.exceptions.StorageException
import java.nio.ByteBuffer
import java.util.logging.Level
import java.util.logging.Logger

class ContentEmptyException : StorageException("Content is empty.")

internal class ContentOps(
    private val blockSpace: BlockSpace
) {

    private fun write(start: BlockID, content: ByteArray): BlockID {
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

    fun write(content: ByteArray): BlockID {
        val firstEmptySpace = blockSpace.findNextEmptyBlock(-1) ?: throw NoEmptySpace(Space.BLOCKS)
        return try {
            write(firstEmptySpace, content)
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Can't write a file. First blockID $firstEmptySpace. Content size: ${content.size}", e)
            delete(firstEmptySpace)
            throw e
        }
    }

    fun read(start: BlockID): ByteArray {
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
            currentBlock = try {
                blockSpace.readBlock(currentBlock.nextBlock)
            } catch (e: Exception){
                logger.log(Level.SEVERE, "Can't read block for removing. blockID: ${currentBlock.nextBlock}", e)
                return
            }
        }
    }

    fun append(start: BlockID, content: ByteArray) {
        //find last block
        var currentBlock = blockSpace.readBlock(start)
        while (currentBlock.nextBlock != EB) {
            currentBlock = blockSpace.readBlock(currentBlock.nextBlock)
        }

        write(currentBlock.blockID, currentBlock.getContent() + content)
    }

    fun getFreeSpace(): Long {
        return blockSpace.getFreeSpace()
    }

    companion object {
        private val logger: Logger = Logger.getLogger(ContentOps::class.java.simpleName)
    }
}