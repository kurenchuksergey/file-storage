package com.ks.storage.file.space.blocks

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.RandomAccessFile
import java.nio.charset.Charset
import java.nio.file.Files
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class BlockSpaceTest {
    @Test
    fun `content should be the same (write and read)`() {
        val tempFile = Files.createTempFile("content-should-be-the-same", "txt")
        RandomAccessFile(tempFile.toFile(), "rw").use {
            val channel = it.channel
            val space = BlockSpace(
                blockCount = 16,
                blockSize = 228,
                offset = 0,
                channel = channel
            )

            val newBlock = space.createEmptyBlock(1)
            newBlock.fill("Test".toByteArray(Charset.defaultCharset()), 0)
            space.writeBlock(newBlock)
            assertEquals(newBlock, space.readBlock(newBlock.blockID))
        }
    }

    @Test
    fun `read access to empty space should be forbidden`() {
        val tempFile = Files.createTempFile("read-access-to-empty-space", "txt")
        RandomAccessFile(tempFile.toFile(), "rw").use {
            val channel = it.channel
            val space = BlockSpace(
                blockCount = 16,
                blockSize = 228,
                offset = 0,
                channel = channel
            )

            assertThrows<Throwable> { space.readBlock(1) }
        }
    }

    @Test
    fun `block shouldn't be free after write but should be after delete`() {
        val tempFile = Files.createTempFile("block-free-test", "txt")
        RandomAccessFile(tempFile.toFile(), "rw").use {
            val channel = it.channel
            val space = BlockSpace(
                blockCount = 16,
                blockSize = 228,
                offset = 0,
                channel = channel
            )

            val newBlock = space.createEmptyBlock(1)
            newBlock.fill("Test".toByteArray(Charset.defaultCharset()), 0)

            space.writeBlock(newBlock)
            assertFalse("after write block should be occupied") {
                space.isEmpty(newBlock.blockID)
            }

            space.delete(newBlock.blockID)
            assertTrue("after delete operation block should be available") {
                space.isEmpty(newBlock.blockID)
            }
        }
    }

    @Test
    fun `space should be the same after reopen source container`() {
        val tempFile = Files.createTempFile("space-restore-test", "txt")

        var block: Block
        RandomAccessFile(tempFile.toFile(), "rw").use {
            val channel = it.channel
            BlockSpace(
                blockCount = 16,
                blockSize = 228,
                offset = 0,
                channel = channel
            ).use { space ->
                block = space.createEmptyBlock(1)
                block.fill("Test".toByteArray(Charset.defaultCharset()), 0)

                space.writeBlock(block)
            }
        }

        RandomAccessFile(tempFile.toFile(), "rw").use {
            val channel = it.channel
            BlockSpace(
                blockCount = 16,
                blockSize = 228,
                offset = 0,
                channel = channel
            ).use { space ->
                assertFalse { space.isEmpty(block.blockID) }
                assertEquals(block, space.readBlock(block.blockID))
            }
        }
    }
}