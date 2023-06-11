package com.ks.storage.file.space.meta

import org.junit.jupiter.api.Test
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.file.Files
import kotlin.test.assertEquals

class MetaTest {
    @Test
    fun `rw to file`(){
        val tempFile = Files.createTempFile("rw-", "txt")
        val source = Meta(
            blockCounter = 8 * 100,
            blockSize = 1024 * 100, //100KB
            hierarchySpaceSize = 2 * 1024 * 1024
        )
        RandomAccessFile(tempFile.toFile(), "rw").use {
            it.channel.position(1).write(ByteBuffer.wrap(source.serialize()))
        }

        RandomAccessFile(tempFile.toFile(), "rw").use {
            val result = Meta.loadFromFile(it.channel, 1)
            assertEquals(source, result)
        }
    }
}