package com.ks.storage.file.space.meta

import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * @param blockCounter - number of block, maximum storage size will be Int.MAX_VALUE * (blockSize - block meta). Pow of the 8
 * @param blockSize - size of each block in bytes
 * @param hierarchySpaceSize - size of hierarchy space (place where we will store file hierarchy)
 */

data class Meta(
    val blockCounter: Int,
    val blockSize: Int = 4 * 1024,
    val hierarchySpaceSize: Int = 1024 * 1024, //1MB
) {
    init {
        assert(blockCounter % 8 == 0) {
            "blockCounter should be a pow of 8"
        }
    }

    fun serialize(): ByteArray {
        val byteBuffer = ByteBuffer.allocate(OBJECT_SIZE)
        return byteBuffer
            .putInt(blockCounter)
            .putInt(blockSize)
            .putInt(hierarchySpaceSize)
            .array()
    }

    companion object {

        const val OBJECT_SIZE = Int.SIZE_BYTES * 3

        fun deserialize(data: ByteArray): Meta {
            val buffer = ByteBuffer.wrap(data)
            val blockCounter = buffer.getInt()
            val blockSize = buffer.getInt()
            val hierarchySpaceSize = buffer.getInt()

            return Meta(
                blockCounter = blockCounter,
                blockSize = blockSize,
                hierarchySpaceSize = hierarchySpaceSize
            )
        }

        fun loadFromFile(channel: FileChannel, offset: Long): Meta {
            val byteBuffer = ByteBuffer.allocate(OBJECT_SIZE)
            channel.position(offset).read(byteBuffer)
            return deserialize(byteBuffer.array())
        }
    }
}