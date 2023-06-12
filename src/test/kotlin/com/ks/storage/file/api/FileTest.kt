package com.ks.storage.file.api

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path


class FileTest {
    @Test
    fun `export import should work`(){
        val sourceData = FileTest::class.java.classLoader.getResourceAsStream("fixture.jpg").use {
            readResource(it!!)
        }
        val file = File( "/testFile", sourceData)
        val output = Files.createTempFile("fixture-import-export", ".jpg")

        file.export(output)

        assertTrue(sourceData.contentEquals(readFile(output)))
    }

    private fun readFile(path: Path): ByteArray {
        val file = path.toFile()
        return FileInputStream(file).use {
            val fileData = ByteArray(file.length().toInt())
            it.read(fileData)
            it.close()
            fileData
        }
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