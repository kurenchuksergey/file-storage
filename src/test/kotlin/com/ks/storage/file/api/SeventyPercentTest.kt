package com.ks.storage.file.api

import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.file.Files
import kotlin.math.roundToInt
import kotlin.test.assertTrue

class SeventyPercentTest {

    @Test
    fun `write (70percent) - delete - write - read`() {
        val sourceData = FileTest::class.java.classLoader.getResourceAsStream("fixture.jpg").use {
            readResource(it!!)
        }

        val path = Files.createTempFile("storage", ".data")

        val localStorage = Storage.new()
            .efficient()
            .path(path)
            .size(sourceData.size + (sourceData.size * 0.3).roundToInt()) //70%
            .smallHierarchy()
            .build()

        val sourceFile = File("/testFolder/test.jpg", sourceData)

        localStorage.createFile(sourceFile)
        localStorage.delete(sourceFile.path)
        localStorage.createFile(sourceFile)
        assertTrue(sourceData.contentEquals(localStorage.getFile(sourceFile.path).content))
    }

    @Test
    fun `write (99percent) - delete - write - read`() {
        val sourceData = FileTest::class.java.classLoader.getResourceAsStream("fixture.jpg").use {
            readResource(it!!)
        }

        val path = Files.createTempFile("storage", ".data")

        val localStorage = Storage.new()
            .efficient()
            .path(path)
            .size(sourceData.size + (sourceData.size * 0.01).roundToInt()) //99%
            .smallHierarchy()
            .build()

        val sourceFile = File("/testFolder/test.jpg", sourceData)

        localStorage.createFile(sourceFile)
        localStorage.delete(sourceFile.path)
        localStorage.createFile(sourceFile)
        assertTrue(sourceData.contentEquals(localStorage.getFile(sourceFile.path).content))
    }

    private fun readResource(stream: InputStream): ByteArray {
        val buffer = ByteArrayOutputStream()

        var nRead: Int
        val data = ByteArray(16384)

        while (stream.read(data, 0, data.size).also { nRead = it } != -1) {
            buffer.write(data, 0, nRead)
        }

        return buffer.toByteArray()
    }
}