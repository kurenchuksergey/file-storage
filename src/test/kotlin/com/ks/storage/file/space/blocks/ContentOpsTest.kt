package com.ks.storage.file.space.blocks

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.io.RandomAccessFile
import java.nio.charset.Charset
import java.nio.file.Files
import kotlin.test.assertTrue

internal class ContentOpsTest {

    @Test
    fun `small content wr test`() {

        val tempFile = Files.createTempFile("small-content-rw-test", "txt")

        RandomAccessFile(tempFile.toFile(), "rw").use {
            val channel = it.channel
            val contentOps = ContentOps(
                BlockSpace(
                    2,
                    SMALL_CONTENT.size + 8,
                    0,
                    channel
                )
            )

            val startID = contentOps.write(SMALL_CONTENT)
            val read = contentOps.read(startID)
            assertTrue(SMALL_CONTENT.contentEquals(read), "Content should be the same")
        }
    }

    @Test
    fun `long content should be successful read after write`() {
        val tempFile = Files.createTempFile("long-content-rw-test", "txt")

        RandomAccessFile(tempFile.toFile(), "rw").use {
            val channel = it.channel
            val contentOps = ContentOps(
                BlockSpace(
                    LONG_CONTENT.size / 64 + 1000, //this delta depend on block size and its efficient
                    64,
                    0,
                    channel
                )
            )

            val startID = contentOps.write(LONG_CONTENT)
            val read = contentOps.read(startID)
            assertTrue(LONG_CONTENT.contentEquals(read), "Content should be the same")
        }
    }

    @Test
    fun `should throw not enough space error when file bigger then blockSpace`() {
        val tempFile = Files.createTempFile("not-enough-space-test", "txt")

        RandomAccessFile(tempFile.toFile(), "rw").use {
            val channel = it.channel
            val contentOps = ContentOps(
                BlockSpace(
                    LONG_CONTENT.size - 1,
                    8,
                    0,
                    channel
                )
            )

            assertThrows<Throwable> { contentOps.write(LONG_CONTENT) }
        }
    }


    @Test
    fun `content should be deleted successful, then file should not be accessible`() {
        val tempFile = Files.createTempFile("small-content-rw-test", "txt")

        RandomAccessFile(tempFile.toFile(), "rw").use {
            val channel = it.channel
            val contentOps = ContentOps(
                BlockSpace(
                    2,
                    SMALL_CONTENT.size / 2 + 10,
                    0,
                    channel
                )
            )

            val startID = contentOps.write(SMALL_CONTENT)

            assertDoesNotThrow { contentOps.read(startID) }
            contentOps.delete(startID)
            assertThrows<Throwable> { contentOps.read(startID) }
        }
    }

    @Test
    fun `content should be successful read after append operation`() {
        val tempFile = Files.createTempFile("small-content-rw-test", "txt")

        RandomAccessFile(tempFile.toFile(), "rw").use {
            val channel = it.channel
            val contentOps = ContentOps(
                BlockSpace(
                    4,
                    SMALL_CONTENT.size / 2 + 10,
                    0,
                    channel
                )
            )

            val startID = contentOps.write(SMALL_CONTENT)
            contentOps.append(startID, SMALL_CONTENT)
            assertTrue((SMALL_CONTENT + SMALL_CONTENT).contentEquals(contentOps.read(startID)))
        }
    }

    companion object FIXTURE {
        val SMALL_CONTENT = "Small Test String".toByteArray(charset = Charset.defaultCharset())
        val LONG_CONTENT = "Long Test String\n".repeat(300).toByteArray(charset = Charset.defaultCharset())
    }
}