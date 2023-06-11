package com.ks.storage.file

import java.nio.ByteBuffer
import kotlin.math.min

typealias BlockID = Int

/**
 * Constant which determine end block in sequence
 */
internal val EB: BlockID
    get() = -1

internal class Block(
    @Transient
    val blockID: BlockID,
    @Transient
    private val blockSize: Int,
    var nextBlock: BlockID = EB,
    private var contentSize: Int = 0,
    private val content: ByteArray
) {

    init {
        assert(blockSize >= content.size) {
            "block memory overflow. Source blockSize: $blockSize. Content size: ${content.size}. BlockID: $blockID"
        }

        assert(blockSize >= contentSize) {
            "block memory overflow. Source blockSize: $blockSize. Size: $contentSize. BlockID: $blockID"
        }
    }

    fun fill(source: ByteArray, offset: Int) {
        source.copyInto(destination = content, startIndex = offset, endIndex = min(offset + content.size, source.size))
        contentSize = min(content.size, (source.size - offset))
    }

    fun serialize(): ByteArray {
        return ByteBuffer.allocate(blockSize)
            .putInt(nextBlock)
            .putInt(contentSize)
            .put(content)
            .array()
    }

    fun contentSize() = contentSize

    fun getContent() = if (content.size == contentSize) content else content.sliceArray(0 until contentSize)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Block

        if (blockID != other.blockID) return false
        if (blockSize != other.blockSize) return false
        if (nextBlock != other.nextBlock) return false
        if (contentSize != other.contentSize) return false
        if (content.size == other.content.size) {
            return content.contentEquals(other.content)
        }
        return if (content.size > other.content.size) {
            content.prefixEquals(other.content)
        } else {
            other.content.prefixEquals(content)
        }
    }

    override fun hashCode(): Int {
        var result = blockSize
        result = 31 * result + nextBlock
        result = 31 * result + contentSize
        result = 31 * result + content.contentHashCode()
        return result
    }

    companion object {
        fun deserialize(byteArray: ByteArray, blockID: BlockID): Block {
            val buffer = ByteBuffer.wrap(byteArray)

            val blockSize = byteArray.size
            val nextBlock = buffer.getInt()
            val contentSize = buffer.getInt()
            val content = ByteArray(buffer.remaining())
            buffer.get(content)

            return Block(
                blockSize = blockSize,
                blockID = blockID,
                nextBlock = nextBlock,
                contentSize = contentSize,
                content = content
            )
        }

        operator fun invoke(blockSize: Int, blockID: BlockID): Block {
            assert(blockSize > Int.SIZE_BYTES * 2) {
                "minimum block size is ${Int.SIZE_BYTES * 2} bytes. BlockID: $blockID"
            }
            return Block(
                blockID = blockID,
                blockSize = blockSize,
                content = ByteArray(blockSize - Int.SIZE_BYTES * 2)
            )
        }
    }
}

internal fun ByteArray.prefixEquals(other: ByteArray): Boolean {
    assert(other.size <= this.size) {
        "Prefix length should be less then source array. Array size: $size, prefix: ${other.size}"
    }
    for (index in other.indices) {
        if (this[index] != other[index]) {
            return false
        }
    }
    return true
}

