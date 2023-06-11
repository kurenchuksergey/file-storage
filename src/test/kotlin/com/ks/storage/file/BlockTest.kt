package com.ks.storage.file

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class BlockTest {

    @Test
    fun `serialized block should be deserializer correctly`() {
        val content = "Test message".toByteArray()
        val source = Block(
            blockID = 1,
            blockSize = 8 * 1024, //8KB
            nextBlock = 228,
            contentSize = content.size,
            content = content
        )

        val serialized = source.serialize()
        val result = Block.deserialize(serialized, 1)
        Assertions.assertEquals(
            source,
            result
        )
        Assertions.assertTrue(result.getContent().prefixEquals(content))
    }

    @Test
    fun `fill content should be deserializer correctly`(){
        val content = "Test message".toByteArray()
        val block = Block(blockSize = 8 * 1024, 1)
        block.fill(content, 0)

        val serialized = block.serialize()
        val result = Block.deserialize(serialized, 1)
        Assertions.assertTrue(result.getContent().prefixEquals(content))
        Assertions.assertEquals(content.size, result.contentSize())
    }

    @Test
    fun `any exception should happen when I overflow block space by long content`() {
        val content = ByteArray(16)
        content.fill('a'.code.toByte())
        Assertions.assertThrows(Throwable::class.java) {
            Block(
                blockID = 1,
                blockSize = 8, //1B
                nextBlock = -1,
                contentSize = content.size,
                content = content
            )
        }
    }
}