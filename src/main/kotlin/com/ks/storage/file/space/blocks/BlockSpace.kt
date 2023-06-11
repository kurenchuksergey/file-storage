package com.ks.storage.file.space.blocks

import com.ks.storage.file.fromStart
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.math.max

internal typealias UsedSpaceMap = BitSet

fun UsedSpaceMap.markUse(blockID: BlockID) {
    this[blockID] = true
}

fun UsedSpaceMap.markFree(blockID: BlockID) {
    this[blockID] = false
}

fun UsedSpaceMap.isFree(blockID: BlockID) = !this[blockID]

internal class BlockSpace(
    private val blockCount: Int,
    private val blockSize: Int, //byte
    private val offset: Long,
    private val channel: FileChannel,
) : Closeable {

    private val spaceMap: UsedSpaceMap

    init {
        val buffer = ByteBuffer.allocate(blockCount / 8) //in bytes
        channel.position(offset).read(buffer)
        spaceMap = UsedSpaceMap.valueOf(buffer.fromStart())
    }

    fun createEmptyBlock(blockID: BlockID) = Block(blockSize, blockID)

    fun findNextEmptyBlock(offset: Int): BlockID? {
        for (index in max(offset, 0) until blockCount) {
            if (spaceMap.isFree(index)) {
                return index
            }
        }
        return null
    }

    fun writeBlock(block: Block) {
        assert(block.blockID in 0..blockCount) {
            "Block: ${block.blockID} doesn't exist"
        }
        spaceMap.markUse(block.blockID)

        val flushedBytes = channel
            .moveToBlock(blockID = block.blockID)
            .write(ByteBuffer.wrap(block.serialize()))
        assert(flushedBytes == blockSize) {
            "content should be flushed fully. Flushed: $flushedBytes, blockID: ${block.blockID}"
        }
        logger.log(Level.FINEST) {
            "block:${block.blockID} was recorded. Content Size: ${block.contentSize()}"
        }
    }

    fun readBlock(blockID: BlockID): Block {
        assert(blockID in 0..blockCount) {
            "Block: $blockID doesn't exist"
        }
        assert(!spaceMap.isFree(blockID)) {
            "Block: $blockID is empty"
        }
        val buffer = ByteBuffer.allocate(blockSize)
        channel.moveToBlock(blockID).read(buffer)
        return Block.deserialize(buffer.array(), blockID).also {
            logger.log(Level.FINEST) {
                "block:${it.blockID} was read. Content Size: ${it.contentSize()}"
            }
        }
    }

    fun isEmpty(blockID: BlockID) = spaceMap.isFree(blockID)

    fun delete(blockID: BlockID) {
        assert(blockID in 0..blockCount) {
            "Block: $blockID doesn't exist"
        }
        spaceMap.markFree(blockID)
        logger.log(Level.FINEST) {
            "block:${blockID} was removed."
        }
    }

    private fun FileChannel.moveToBlock(blockID: BlockID) =
        this.position(offset + blockCount / 8 + blockSize * blockID)


    private fun saveSpaceMap() {
        channel.position(offset).write(
            ByteBuffer.allocate(blockCount / 8).put(spaceMap.toByteArray()).fromStart()
        )
        logger.log(Level.FINEST) {
            "UsedSpaceMap was flushed"
        }
    }

    override fun close() {
        saveSpaceMap()
    }

    companion object {
        private val logger = Logger.getLogger(BlockSpace::class.java.canonicalName)
    }
}